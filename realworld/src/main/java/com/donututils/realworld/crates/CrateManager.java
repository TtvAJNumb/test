package com.donututils.realworld.crates;

import com.donututils.realworld.crates.db.DatabaseManager;
import com.donututils.realworld.ledger.economy.LedgerEconomyProvider;
import com.donututils.realworld.ledger.shards.ShardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Crates: bind a chest/trapped-chest/barrel/ender-chest/shulker-box in the world to a named crate,
 * hand out physical keys (an ordinary item tagged via PDC - see {@link CrateKeys}), and right-click to
 * open one with a matching key, rolling a weighted reward from that crate's pool. A from-scratch
 * native replacement for what CrateBindAddon used to bridge into the real UltimateDonutSmp for.
 */
public final class CrateManager {

    private record BlockKey(String world, int x, int y, int z) {
        static BlockKey of(Block block) {
            return new BlockKey(block.getWorld().getName(), block.getLocation().getBlockX(),
                    block.getLocation().getBlockY(), block.getLocation().getBlockZ());
        }
    }

    private final Plugin plugin;
    private final DatabaseManager database;
    private final CrateKeys keys;
    private final CrateItemFactory itemFactory;
    private final LedgerEconomyProvider economy;
    private final ShardManager shardManager;
    private final Supplier<CrateConfig> configSupplier;
    private final Random random = new Random();

    private final Map<BlockKey, String> boundBlocks = new ConcurrentHashMap<>();
    private final Map<BlockKey, Entity> holograms = new ConcurrentHashMap<>();

    public CrateManager(Plugin plugin, DatabaseManager database, CrateKeys keys, CrateItemFactory itemFactory,
                         LedgerEconomyProvider economy, ShardManager shardManager, Supplier<CrateConfig> configSupplier) {
        this.plugin = plugin;
        this.database = database;
        this.keys = keys;
        this.itemFactory = itemFactory;
        this.economy = economy;
        this.shardManager = shardManager;
        this.configSupplier = configSupplier;
        loadAll();
    }

    private void loadAll() {
        String sql = "SELECT world, x, y, z, crate_id FROM bound_blocks";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                BlockKey key = new BlockKey(rows.getString("world"), rows.getInt("x"), rows.getInt("y"), rows.getInt("z"));
                boundBlocks.put(key, rows.getString("crate_id"));
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load bound crate blocks", ex);
        }
        plugin.getLogger().info("Loaded " + boundBlocks.size() + " bound crate block(s).");
    }

    /** Re-spawns every bound block's floating name tag - call once worlds are loaded (e.g. on enable). */
    public void respawnHolograms() {
        for (Map.Entry<BlockKey, String> entry : boundBlocks.entrySet()) {
            Location loc = toLocation(entry.getKey());
            if (loc != null && loc.getWorld() != null) {
                spawnHologram(entry.getKey(), loc, entry.getValue());
            }
        }
    }

    public boolean isBindable(Material material) {
        String n = material.name();
        return n.equals("CHEST") || n.equals("TRAPPED_CHEST") || n.equals("BARREL")
                || n.equals("ENDER_CHEST") || n.contains("SHULKER_BOX");
    }

    public String boundCrateId(Block block) {
        return boundBlocks.get(BlockKey.of(block));
    }

    public boolean bind(Block block, String crateId) {
        if (configSupplier.get().crate(crateId) == null) {
            return false;
        }
        BlockKey key = BlockKey.of(block);
        boundBlocks.put(key, crateId);
        persistAsync(key, crateId);
        spawnHologram(key, block.getLocation(), crateId);
        return true;
    }

    public boolean unbind(Block block) {
        BlockKey key = BlockKey.of(block);
        String removed = boundBlocks.remove(key);
        if (removed == null) {
            return false;
        }
        deleteAsync(key);
        removeHologram(key);
        return true;
    }

    public void giveKeys(Player player, String crateId, int amount) {
        CrateDefinition definition = configSupplier.get().crate(crateId);
        if (definition == null) {
            return;
        }
        player.getInventory().addItem(itemFactory.createKey(definition, amount));
    }

    /** Called on right-clicking a bound block: consumes one matching key from the player's hand (or
     * inventory) and rolls a reward. Returns false (no-op, no message sent by this method) if the
     * player has no key for that crate. */
    public boolean tryOpen(Player player, String crateId) {
        CrateDefinition definition = configSupplier.get().crate(crateId);
        if (definition == null || definition.rewards().isEmpty()) {
            return false;
        }
        if (!consumeKey(player, crateId)) {
            return false;
        }
        CrateReward reward = rollReward(definition);
        grant(player, reward);
        player.sendMessage(color("&a&lCrate opened! &7You got: &f" + reward.displayName()));
        return true;
    }

    private boolean consumeKey(Player player, String crateId) {
        PlayerInventory inventory = player.getInventory();
        ItemStack hand = inventory.getItemInMainHand();
        if (itemFactory.isKeyFor(hand, crateId)) {
            hand.setAmount(hand.getAmount() - 1);
            return true;
        }
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (itemFactory.isKeyFor(stack, crateId)) {
                if (stack.getAmount() <= 1) {
                    inventory.setItem(i, null);
                } else {
                    stack.setAmount(stack.getAmount() - 1);
                }
                return true;
            }
        }
        return false;
    }

    private CrateReward rollReward(CrateDefinition definition) {
        int totalWeight = 0;
        for (CrateReward reward : definition.rewards()) {
            totalWeight += Math.max(0, reward.weight());
        }
        int roll = totalWeight <= 0 ? 0 : random.nextInt(totalWeight);
        int cursor = 0;
        for (CrateReward reward : definition.rewards()) {
            cursor += Math.max(0, reward.weight());
            if (roll < cursor) {
                return reward;
            }
        }
        return definition.rewards().get(definition.rewards().size() - 1);
    }

    private void grant(Player player, CrateReward reward) {
        switch (reward.kind()) {
            case MONEY -> economy.depositPlayer(player, reward.moneyAmount());
            case SHARDS -> shardManager.credit(player.getUniqueId(), reward.shardsAmount());
            case ITEM -> {
                int amount = reward.itemAmountMin() >= reward.itemAmountMax() ? reward.itemAmountMin()
                        : reward.itemAmountMin() + random.nextInt(reward.itemAmountMax() - reward.itemAmountMin() + 1);
                player.getInventory().addItem(new ItemStack(reward.material(), amount));
            }
        }
    }

    private void spawnHologram(BlockKey key, Location blockLocation, String crateId) {
        removeHologram(key);
        CrateDefinition definition = configSupplier.get().crate(crateId);
        String label = definition != null ? definition.displayName() : crateId;
        Location above = new Location(blockLocation.getWorld(), blockLocation.getX() + 0.5,
                blockLocation.getY() + 1.3, blockLocation.getZ() + 0.5);
        ArmorStand stand = blockLocation.getWorld().spawn(above, ArmorStand.class);
        stand.setInvisible(true);
        stand.setMarker(true);
        stand.setSmall(true);
        stand.setBasePlate(false);
        stand.setGravity(false);
        stand.setCustomName(color("&e&l" + label + " Crate"));
        stand.setCustomNameVisible(true);
        holograms.put(key, stand);
    }

    private void removeHologram(BlockKey key) {
        Entity existing = holograms.remove(key);
        if (existing != null) {
            existing.remove();
        }
    }

    private Location toLocation(BlockKey key) {
        var world = Bukkit.getWorld(key.world());
        return world == null ? null : new Location(world, key.x(), key.y(), key.z());
    }

    private void persistAsync(BlockKey key, String crateId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO bound_blocks (world, x, y, z, crate_id) VALUES (?, ?, ?, ?, ?) "
                    + "ON CONFLICT(world, x, y, z) DO UPDATE SET crate_id = excluded.crate_id";
            try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, key.world());
                statement.setInt(2, key.x());
                statement.setInt(3, key.y());
                statement.setInt(4, key.z());
                statement.setString(5, crateId);
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to persist crate bind at " + key, ex);
            }
        });
    }

    private void deleteAsync(BlockKey key) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "DELETE FROM bound_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?";
            try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, key.world());
                statement.setInt(2, key.x());
                statement.setInt(3, key.y());
                statement.setInt(4, key.z());
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to delete crate bind at " + key, ex);
            }
        });
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
