package com.donututils.donutrep.market.service;

import com.donututils.donutrep.economy.EconomyManager;
import com.donututils.donutrep.economy.ShardManager;
import com.donututils.donutrep.market.config.BoutiqueItem;
import com.donututils.donutrep.market.config.CatalogEntry;
import com.donututils.donutrep.market.config.MarketConfig;
import com.donututils.donutrep.market.pricing.MarketPricingEngine;
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

/** All buy/sell logic for the Market: the generated vanilla catalog (dynamic Money pricing) and the
 * Shard Boutique (black-market vanilla items, priced in Shards). */
public final class MarketService {

    public record TradeResult(boolean success, String message) {
        static TradeResult fail(String message) {
            return new TradeResult(false, message);
        }
    }

    private final Map<String, CatalogEntry> byMaterial;
    private final MarketPricingEngine pricing;
    private final EconomyManager economy;
    private final ShardManager shardManager;
    private final Supplier<MarketConfig> configSupplier;

    private final Map<String, Long> lastTradeAtMillis = new HashMap<>();

    public MarketService(List<CatalogEntry> catalog, MarketPricingEngine pricing, EconomyManager economy,
                          ShardManager shardManager, Supplier<MarketConfig> configSupplier) {
        this.byMaterial = new HashMap<>();
        for (CatalogEntry entry : catalog) {
            byMaterial.put(entry.material().name(), entry);
        }
        this.pricing = pricing;
        this.economy = economy;
        this.shardManager = shardManager;
        this.configSupplier = configSupplier;
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

    // ── Shard Boutique ───────────────────────────────────────────────────────────────────────────

    public TradeResult buyBoutiqueItem(Player player, BoutiqueItem item) {
        if (!shardManager.debit(player.getUniqueId(), item.priceShards())) {
            return TradeResult.fail("You don't have " + item.priceShards() + " Shards.");
        }
        ItemStack result;
        try {
            result = new ItemStack(Material.valueOf(item.material().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            shardManager.credit(player.getUniqueId(), item.priceShards());
            return TradeResult.fail("That item isn't configured correctly.");
        }
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', item.displayName()));
            if (item.customModelData() != 0) {
                meta.setCustomModelData(item.customModelData());
            }
            result.setItemMeta(meta);
        }
        player.getInventory().addItem(result);
        return new TradeResult(true, "Bought " + item.displayName() + " for " + item.priceShards() + " Shards.");
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
