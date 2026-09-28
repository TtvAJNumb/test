package com.donututils.realworld.market.service;

import com.donututils.realworld.arsenal.config.ArsenalConfig;
import com.donututils.realworld.arsenal.config.WeaponDefinition;
import com.donututils.realworld.arsenal.weapon.WeaponItemFactory;
import com.donututils.realworld.ledger.economy.LedgerEconomyProvider;
import com.donututils.realworld.ledger.shards.ShardManager;
import com.donututils.realworld.market.config.BoutiqueItem;
import com.donututils.realworld.market.config.CatalogEntry;
import com.donututils.realworld.market.config.MarketConfig;
import com.donututils.realworld.market.pricing.MarketPricingEngine;
import com.donututils.realworld.motors.config.MotorsConfig;
import com.donututils.realworld.motors.config.VehicleDefinition;
import com.donututils.realworld.motors.vehicle.VehicleItemFactory;
import com.donututils.realworld.municipal.permit.PermitManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** All buy/sell logic for the Market: the generated vanilla catalog (dynamic Money pricing),
 * Firearms/Motors/Permits storefronts in front of Arsenal/Motors/Municipal's own item factories and
 * permit system, and the Shard Boutique. Kept as one class since every one of these is a small,
 * self-contained operation and splitting them further would just be indirection. */
public final class MarketService {

    public record TradeResult(boolean success, String message) {
        static TradeResult fail(String message) {
            return new TradeResult(false, message);
        }
    }

    private final Map<String, CatalogEntry> byMaterial;
    private final MarketPricingEngine pricing;
    private final LedgerEconomyProvider economy;
    private final ShardManager shardManager;
    private final Supplier<MarketConfig> configSupplier;
    private final Supplier<ArsenalConfig> arsenalConfigSupplier;
    private final WeaponItemFactory weaponItemFactory;
    private final Supplier<MotorsConfig> motorsConfigSupplier;
    private final VehicleItemFactory vehicleItemFactory;
    private final PermitManager permitManager;

    private final Map<String, Long> lastTradeAtMillis = new HashMap<>();

    public MarketService(List<CatalogEntry> catalog, MarketPricingEngine pricing, LedgerEconomyProvider economy,
                          ShardManager shardManager, Supplier<MarketConfig> configSupplier,
                          Supplier<ArsenalConfig> arsenalConfigSupplier, WeaponItemFactory weaponItemFactory,
                          Supplier<MotorsConfig> motorsConfigSupplier, VehicleItemFactory vehicleItemFactory,
                          PermitManager permitManager) {
        this.byMaterial = new HashMap<>();
        for (CatalogEntry entry : catalog) {
            byMaterial.put(entry.material().name(), entry);
        }
        this.pricing = pricing;
        this.economy = economy;
        this.shardManager = shardManager;
        this.configSupplier = configSupplier;
        this.arsenalConfigSupplier = arsenalConfigSupplier;
        this.weaponItemFactory = weaponItemFactory;
        this.motorsConfigSupplier = motorsConfigSupplier;
        this.vehicleItemFactory = vehicleItemFactory;
        this.permitManager = permitManager;
    }

    public CatalogEntry entryFor(Material material) {
        return byMaterial.get(material.name());
    }

    // ── Vanilla catalog (dynamic pricing) ───────────────────────────────────────────────────────

    public TradeResult buy(Player player, CatalogEntry entry, int quantity) {
        quantity = clampQuantity(quantity);
        if (!checkCooldown(player.getUniqueId(), entry.material())) {
            return TradeResult.fail("Please wait before trading that again.");
        }
        double price = pricing.buyPrice(entry) * quantity;
        if (!economy.has(player, price)) {
            return TradeResult.fail("You can't afford that - costs " + formatMoney(price) + ".");
        }
        economy.withdrawPlayer(player, price);
        player.getInventory().addItem(new ItemStack(entry.material(), quantity));
        pricing.onBuy(entry.material(), quantity);
        return new TradeResult(true, "Bought " + quantity + "x " + displayName(entry.material()) + " for " + formatMoney(price) + ".");
    }

    public TradeResult sell(Player player, Material material, int quantity) {
        CatalogEntry entry = entryFor(material);
        if (entry == null) {
            return TradeResult.fail("The market doesn't buy that.");
        }
        quantity = clampQuantity(quantity);
        if (!hasAtLeast(player, material, quantity)) {
            return TradeResult.fail("You don't have " + quantity + "x " + displayName(material) + ".");
        }
        if (!checkCooldown(player.getUniqueId(), material)) {
            return TradeResult.fail("Please wait before trading that again.");
        }
        removeMaterial(player, material, quantity);
        double price = pricing.sellPrice(entry) * quantity;
        economy.depositPlayer(player, price);
        pricing.onSell(material, quantity);
        return new TradeResult(true, "Sold " + quantity + "x " + displayName(material) + " for " + formatMoney(price) + ".");
    }

    /** Sells every unit of this material the player is currently carrying (used by the shift-click
     * "sell" action in the catalog GUI) - not a fixed/huge quantity, since that would wrongly fail
     * for a player holding less than the per-transaction cap. */
    public TradeResult sellAllHeld(Player player, Material material) {
        int held = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) {
                held += stack.getAmount();
            }
        }
        if (held <= 0) {
            return TradeResult.fail("You don't have any " + displayName(material) + ".");
        }
        return sell(player, material, held);
    }

    public TradeResult sellHand(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            return TradeResult.fail("You're not holding anything.");
        }
        return sell(player, hand.getType(), hand.getAmount());
    }

    // ── Firearms & Ammunition ────────────────────────────────────────────────────────────────────

    public TradeResult buyWeapon(Player player, String weaponId) {
        WeaponDefinition definition = arsenalConfigSupplier.get().weapon(weaponId);
        if (definition == null) {
            return TradeResult.fail("Unknown weapon.");
        }
        if (!economy.has(player, definition.priceMoney())) {
            return TradeResult.fail("You can't afford that - costs " + formatMoney(definition.priceMoney()) + ".");
        }
        economy.withdrawPlayer(player, definition.priceMoney());
        player.getInventory().addItem(weaponItemFactory.createWeapon(definition));
        return new TradeResult(true, "Bought a " + definition.displayName() + " for " + formatMoney(definition.priceMoney()) + ".");
    }

    public TradeResult buyAmmo(Player player, String weaponId) {
        WeaponDefinition definition = arsenalConfigSupplier.get().weapon(weaponId);
        if (definition == null) {
            return TradeResult.fail("Unknown weapon.");
        }
        if (!economy.has(player, definition.ammoPriceMoney())) {
            return TradeResult.fail("You can't afford that - costs " + formatMoney(definition.ammoPriceMoney()) + ".");
        }
        economy.withdrawPlayer(player, definition.ammoPriceMoney());
        player.getInventory().addItem(weaponItemFactory.createAmmo(definition, definition.magazineSize()));
        return new TradeResult(true, "Bought " + definition.ammoDisplayName() + " for " + formatMoney(definition.ammoPriceMoney()) + ".");
    }

    // ── Motors & Vehicles ────────────────────────────────────────────────────────────────────────

    public TradeResult buyVehicle(Player player, String vehicleId) {
        VehicleDefinition definition = motorsConfigSupplier.get().vehicle(vehicleId);
        if (definition == null) {
            return TradeResult.fail("Unknown vehicle.");
        }
        if (!economy.has(player, definition.priceMoney())) {
            return TradeResult.fail("You can't afford that - costs " + formatMoney(definition.priceMoney()) + ".");
        }
        economy.withdrawPlayer(player, definition.priceMoney());
        player.getInventory().addItem(vehicleItemFactory.createSpawnerKey(definition));
        return new TradeResult(true, "Bought a " + definition.displayName() + " spawner for " + formatMoney(definition.priceMoney()) + ".");
    }

    // ── Municipal & Corporate Permits ────────────────────────────────────────────────────────────

    public TradeResult buyPermit(Player player, String permitType) {
        PermitManager.PurchaseResult result = permitManager.buy(player, permitType);
        return new TradeResult(result.success(), result.message());
    }

    // ── Shard Boutique ───────────────────────────────────────────────────────────────────────────

    public TradeResult buyBoutiqueItem(Player player, BoutiqueItem item) {
        if (!shardManager.debit(player.getUniqueId(), item.priceShards())) {
            return TradeResult.fail("You don't have " + item.priceShards() + " Shards.");
        }
        ItemStack result;
        if (item.isWeaponSkin()) {
            WeaponDefinition base = arsenalConfigSupplier.get().weapon(item.baseWeapon());
            if (base == null) {
                shardManager.credit(player.getUniqueId(), item.priceShards());
                return TradeResult.fail("That skin's base weapon isn't configured anymore.");
            }
            result = weaponItemFactory.createWeapon(base);
            applySkinModelData(result, item.customModelData(), item.displayName());
        } else if (item.isVehicleSkin()) {
            VehicleDefinition base = motorsConfigSupplier.get().vehicle(item.baseVehicle());
            if (base == null) {
                shardManager.credit(player.getUniqueId(), item.priceShards());
                return TradeResult.fail("That skin's base vehicle isn't configured anymore.");
            }
            result = vehicleItemFactory.createSpawnerKey(base);
            applySkinModelData(result, item.customModelData(), item.displayName());
        } else {
            try {
                result = new ItemStack(Material.valueOf(item.material().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                shardManager.credit(player.getUniqueId(), item.priceShards());
                return TradeResult.fail("That item isn't configured correctly.");
            }
            ItemMeta meta = result.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', item.displayName()));
                result.setItemMeta(meta);
            }
        }
        player.getInventory().addItem(result);
        return new TradeResult(true, "Bought " + item.displayName() + " for " + item.priceShards() + " Shards.");
    }

    private void applySkinModelData(ItemStack item, int customModelData, String displayName) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(customModelData);
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', displayName));
            item.setItemMeta(meta);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────────────────────

    private boolean checkCooldown(UUID playerId, Material material) {
        String key = playerId + ":" + material.name();
        long now = System.currentTimeMillis();
        long cooldownMillis = configSupplier.get().transactionCooldownSeconds() * 1000L;
        Long last = lastTradeAtMillis.get(key);
        if (last != null && now - last < cooldownMillis) {
            return false;
        }
        lastTradeAtMillis.put(key, now);
        return true;
    }

    private int clampQuantity(int quantity) {
        return Math.max(1, Math.min(quantity, configSupplier.get().maxQuantityPerTransaction()));
    }

    private boolean hasAtLeast(Player player, Material material, int quantity) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) {
                total += stack.getAmount();
                if (total >= quantity) {
                    return true;
                }
            }
        }
        return false;
    }

    private void removeMaterial(Player player, Material material, int quantity) {
        ItemStack[] contents = player.getInventory().getContents();
        int remaining = quantity;
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != material) {
                continue;
            }
            int take = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            remaining -= take;
            player.getInventory().setItem(i, stack.getAmount() <= 0 ? null : stack);
        }
    }

    private static String displayName(Material material) {
        String[] words = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }

    private static String formatMoney(double amount) {
        return "$" + String.format(Locale.US, "%,.2f", amount);
    }
}
