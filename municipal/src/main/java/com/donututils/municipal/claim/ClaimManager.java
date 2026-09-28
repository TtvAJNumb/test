package com.donututils.municipal.claim;

import com.donututils.municipal.config.MunicipalConfig;
import com.donututils.municipal.db.DatabaseManager;
import com.donututils.municipal.economy.VaultEconomyBridge;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Grid-based chunk claiming, built entirely inside Municipal - no third-party claims plugin. A claim
 * is one 16x16 vanilla chunk; ownership and the per-owner missed-property-tax streak both live in
 * memory (loaded at startup) for fast reads on every block interaction, with SQLite as the durable
 * backing store.
 *
 * <p>{@link #claim(Player)} and {@link #unclaim(Player)} each block on a database write to get a
 * definite success/failure result back - callers MUST invoke them from an async task, never directly
 * from a command handler on the main thread, then hop back with {@code runTask} to message the player.
 * {@link #isProtected}, {@link #owner}, and {@link #renderMap} are pure in-memory reads and are safe
 * to call from the main thread (e.g. from a block-event listener).
 */
public final class ClaimManager {

    private final Plugin plugin;
    private final DatabaseManager database;
    private final Supplier<MunicipalConfig> configSupplier;

    private final Map<ChunkKey, UUID> claimedChunks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> missedTicks = new ConcurrentHashMap<>();

    public ClaimManager(Plugin plugin, DatabaseManager database, Supplier<MunicipalConfig> configSupplier) {
        this.plugin = plugin;
        this.database = database;
        this.configSupplier = configSupplier;
        loadAll();
    }

    private void loadAll() {
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT world, chunk_x, chunk_z, owner_id FROM claims")) {
            while (rows.next()) {
                ChunkKey key = new ChunkKey(rows.getString("world"), rows.getInt("chunk_x"), rows.getInt("chunk_z"));
                try {
                    claimedChunks.put(key, UUID.fromString(rows.getString("owner_id")));
                } catch (IllegalArgumentException ignored) {
                    // malformed owner id - skip
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load claims", ex);
        }

        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT owner_id, missed_ticks FROM claim_tax_status")) {
            while (rows.next()) {
                try {
                    missedTicks.put(UUID.fromString(rows.getString("owner_id")), rows.getInt("missed_ticks"));
                } catch (IllegalArgumentException ignored) {
                    // malformed owner id - skip
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load claim tax status", ex);
        }

        plugin.getLogger().info("Loaded " + claimedChunks.size() + " claimed chunk(s).");
    }

    public record ClaimResult(boolean success, String message) {
    }

    /** Blocks on a database insert - see class Javadoc. */
    public ClaimResult claim(Player player, VaultEconomyBridge economy) {
        Location loc = player.getLocation();
        ChunkKey key = ChunkKey.of(loc.getChunk());

        if (claimedChunks.containsKey(key)) {
            UUID existingOwner = claimedChunks.get(key);
            if (existingOwner.equals(player.getUniqueId())) {
                return new ClaimResult(false, "You already own this chunk.");
            }
            return new ClaimResult(false, "This chunk is already claimed by someone else.");
        }

        if (!player.hasPermission("municipal.permit.building")) {
            return new ClaimResult(false, "You need a building permit to claim land - see /permit buy building.");
        }

        MunicipalConfig config = configSupplier.get();
        double fee = config.claimFee();
        if (!economy.has(player.getUniqueId(), fee)) {
            return new ClaimResult(false, "You need " + formatMoney(fee) + " to claim this chunk.");
        }
        VaultEconomyBridge.EconomyResult withdrawal = economy.withdraw(player.getUniqueId(), fee);
        if (!withdrawal.success()) {
            return new ClaimResult(false, withdrawal.errorMessage() != null ? withdrawal.errorMessage() : "Payment failed.");
        }

        String sql = "INSERT INTO claims (world, chunk_x, chunk_z, owner_id, claimed_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key.world());
            statement.setInt(2, key.x());
            statement.setInt(3, key.z());
            statement.setString(4, player.getUniqueId().toString());
            statement.setLong(5, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to persist claim", ex);
            economy.deposit(player.getUniqueId(), fee);
            return new ClaimResult(false, "Something went wrong saving your claim - you were refunded.");
        }

        claimedChunks.put(key, player.getUniqueId());
        return new ClaimResult(true, "Claimed chunk (" + key.x() + ", " + key.z() + ") in " + key.world()
                + " for " + formatMoney(fee) + ".");
    }

    /** Blocks on a database delete - see class Javadoc. */
    public ClaimResult unclaim(Player player) {
        ChunkKey key = ChunkKey.of(player.getLocation().getChunk());
        UUID owner = claimedChunks.get(key);
        if (owner == null) {
            return new ClaimResult(false, "This chunk isn't claimed.");
        }
        if (!owner.equals(player.getUniqueId()) && !player.hasPermission("municipal.bypass")) {
            return new ClaimResult(false, "You don't own this chunk.");
        }

        if (!deleteClaimRow(key)) {
            return new ClaimResult(false, "Something went wrong releasing this claim.");
        }
        claimedChunks.remove(key);
        return new ClaimResult(true, "Released chunk (" + key.x() + ", " + key.z() + ") back to the public domain.");
    }

    private boolean deleteClaimRow(ChunkKey key) {
        String sql = "DELETE FROM claims WHERE world = ? AND chunk_x = ? AND chunk_z = ?";
        try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key.world());
            statement.setInt(2, key.x());
            statement.setInt(3, key.z());
            statement.executeUpdate();
            return true;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete claim", ex);
            return false;
        }
    }

    /** Pure in-memory read - safe from the main thread. */
    public UUID owner(Chunk chunk) {
        return claimedChunks.get(ChunkKey.of(chunk));
    }

    /** Pure in-memory read - safe from the main thread. True if this chunk is claimed by someone
     * other than {@code player}, and they don't hold the bypass permission. */
    public boolean isProtected(Location location, Player player) {
        if (player.hasPermission("municipal.bypass")) {
            return false;
        }
        UUID owner = claimedChunks.get(ChunkKey.of(location.getChunk()));
        return owner != null && !owner.equals(player.getUniqueId());
    }

    /** Renders a small ASCII grid of claim ownership around a location, for chat. */
    public List<String> renderMap(Location center, Player viewer, int radiusChunks) {
        Chunk originChunk = center.getChunk();
        String world = originChunk.getWorld().getName();
        int originX = originChunk.getX();
        int originZ = originChunk.getZ();

        List<String> lines = new java.util.ArrayList<>();
        for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
            StringBuilder row = new StringBuilder();
            for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
                if (dx == 0 && dz == 0) {
                    row.append("&e[X]");
                    continue;
                }
                UUID owner = claimedChunks.get(new ChunkKey(world, originX + dx, originZ + dz));
                if (owner == null) {
                    row.append("&7[ ]");
                } else if (owner.equals(viewer.getUniqueId())) {
                    row.append("&a[#]");
                } else {
                    row.append("&c[#]");
                }
            }
            lines.add(row.toString());
        }
        return lines;
    }

    public int claimCountOf(UUID ownerId) {
        int count = 0;
        for (UUID owner : claimedChunks.values()) {
            if (owner.equals(ownerId)) {
                count++;
            }
        }
        return count;
    }

    /** Scheduled on an async repeating task. Bills every claim owner property tax proportional to
     * their chunk count; an owner who can't pay accrues a missed-tick streak, and is foreclosed
     * (all their chunks released back to the public domain) once that streak passes
     * {@code claims.foreclosure-after-missed-ticks}. */
    public void tickPropertyTax(VaultEconomyBridge economy) {
        MunicipalConfig config = configSupplier.get();
        Map<UUID, Integer> chunksByOwner = new HashMap<>();
        for (UUID owner : claimedChunks.values()) {
            chunksByOwner.merge(owner, 1, Integer::sum);
        }

        for (Map.Entry<UUID, Integer> entry : chunksByOwner.entrySet()) {
            UUID ownerId = entry.getKey();
            double tax = entry.getValue() * config.taxPerChunk();

            VaultEconomyBridge.EconomyResult result = economy.withdraw(ownerId, tax);
            if (result.success()) {
                setMissedTicks(ownerId, 0);
                continue;
            }

            int missed = missedTicks.getOrDefault(ownerId, 0) + 1;
            if (missed >= config.foreclosureAfterMissedTicks()) {
                foreclose(ownerId);
                notifyIfOnline(ownerId, "&cYour unpaid property tax has resulted in foreclosure - your claimed land has been released.");
            } else {
                setMissedTicks(ownerId, missed);
                notifyIfOnline(ownerId, "&cYou couldn't afford your property tax bill of " + formatMoney(tax)
                        + " (" + missed + "/" + config.foreclosureAfterMissedTicks() + " missed payments before foreclosure).");
            }
        }
    }

    private void foreclose(UUID ownerId) {
        List<ChunkKey> owned = new java.util.ArrayList<>();
        for (Map.Entry<ChunkKey, UUID> entry : claimedChunks.entrySet()) {
            if (entry.getValue().equals(ownerId)) {
                owned.add(entry.getKey());
            }
        }
        for (ChunkKey key : owned) {
            deleteClaimRow(key);
            claimedChunks.remove(key);
        }
        setMissedTicks(ownerId, 0);
    }

    private void setMissedTicks(UUID ownerId, int missed) {
        missedTicks.put(ownerId, missed);
        String sql = "INSERT INTO claim_tax_status (owner_id, missed_ticks, last_tick_at) VALUES (?, ?, ?) "
                + "ON CONFLICT(owner_id) DO UPDATE SET missed_ticks = excluded.missed_ticks, last_tick_at = excluded.last_tick_at";
        try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ownerId.toString());
            statement.setInt(2, missed);
            statement.setLong(3, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to persist claim tax status for " + ownerId, ex);
        }
    }

    private void notifyIfOnline(UUID playerId, String message) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    private static String formatMoney(double amount) {
        return "$" + String.format(Locale.US, "%,.2f", amount);
    }
}
