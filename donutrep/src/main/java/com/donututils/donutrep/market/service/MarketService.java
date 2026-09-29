package com.donututils.donutrep.market.service;

import com.donututils.donutrep.crates.CrateManager;
import com.donututils.donutrep.economy.EconomyManager;
import com.donututils.donutrep.economy.ShardManager;
import com.donututils.donutrep.market.config.Currency;
import com.donututils.donutrep.market.config.MarketConfig;
import com.donututils.donutrep.market.config.ShopItem;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;
import java.util.function.Supplier;

/** Buy-only logic for the /shop catalog, matching real UDS: no dynamic pricing, no market-side sell-back
 * (instant-sell was removed per the "keep only the 7 addons" scope cut). A purchase either grants a real
 * crate key through the existing Crates subsystem (the CRATE-KEYS category), a simplified, clearly
 * disclosed placeholder spawner item (the SHARD category - no full /spawner system exists yet), or a
 * plain vanilla ItemStack. */
public final class MarketService {

    public record TradeResult(boolean success, String message) {
        static TradeResult fail(String message) {
            return new TradeResult(false, message);
        }
    }

    private final EconomyManager economy;
    private final ShardManager shardManager;
    private final CrateManager crateManager;
    private final Supplier<MarketConfig> configSupplier;

    public MarketService(EconomyManager economy, ShardManager shardManager, CrateManager crateManager,
                          Supplier<MarketConfig> configSupplier) {
        this.economy = economy;
        this.shardManager = shardManager;
        this.crateManager = crateManager;
        this.configSupplier = configSupplier;
    }

    public TradeResult buy(Player player, ShopItem item, int quantity) {
        int cap = item.maxQuantity() > 0 ? item.maxQuantity() : configSupplier.get().maxQuantityPerTransaction();
        quantity = Math.max(1, Math.min(quantity, cap));
        double total = item.price() * quantity;

        if (item.currency() == Currency.MONEY) {
            if (!economy.has(player, total)) {
                return TradeResult.fail("You can't afford that - costs " + formatMoney(total) + ".");
            }
            economy.withdrawPlayer(player, total);
        } else {
            if (!shardManager.debit(player.getUniqueId(), (long) total)) {
                return TradeResult.fail("You don't have " + (long) total + " Shards.");
            }
        }

        if (item.crateId() != null) {
            crateManager.giveKeys(player, item.crateId(), quantity);
            return new TradeResult(true, "Bought " + quantity + "x " + item.displayName() + " for "
                    + formatPrice(item.currency(), total) + ".");
        }

        if (item.spawnerMob() != null) {
            player.getInventory().addItem(placeholderSpawner(item, quantity));
            player.sendMessage(color("&7(Spawner mob type isn't wired to a real /spawner system yet - "
                    + "this is a placeholder item until that's built.)"));
            return new TradeResult(true, "Bought " + quantity + "x " + item.displayName() + " for "
                    + formatPrice(item.currency(), total) + ".");
        }

        player.getInventory().addItem(new ItemStack(item.material(), quantity));
        return new TradeResult(true, "Bought " + quantity + "x " + item.displayName() + " for "
                + formatPrice(item.currency(), total) + ".");
    }

    private ItemStack placeholderSpawner(ShopItem item, int quantity) {
        ItemStack stack = new ItemStack(item.material(), quantity);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&d" + capitalize(item.spawnerMob()) + " Spawner &7(placeholder)"));
            meta.setLore(java.util.List.of(color("&7Not yet wired to a real spawner mechanic.")));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static String capitalize(String mob) {
        String[] words = mob.toLowerCase(Locale.ROOT).split("_");
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

    private static String formatPrice(Currency currency, double amount) {
        return currency == Currency.MONEY ? formatMoney(amount) : (long) amount + " Shards";
    }

    private static String formatMoney(double amount) {
        return "$" + String.format(Locale.US, "%,.2f", amount);
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
