package com.donututils.donutrep.market.command;

import com.donututils.donutrep.market.config.CatalogEntry;
import com.donututils.donutrep.market.pricing.MarketPricingEngine;
import com.donututils.donutrep.market.service.MarketService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/** /worth [material] - shows the current buy/sell price of the item in your hand, or a named
 * material. */
public final class WorthCommand implements CommandExecutor {

    private final MarketService marketService;
    private final MarketPricingEngine pricing;

    public WorthCommand(MarketService marketService, MarketPricingEngine pricing) {
        this.marketService = marketService;
        this.pricing = pricing;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Material material;
        if (args.length >= 1) {
            try {
                material = Material.valueOf(args[0].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(color("&cUnknown material: " + args[0]));
                return true;
            }
        } else if (sender instanceof Player player) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) {
                sender.sendMessage(color("&cHold an item or specify a material: /worth [material]"));
                return true;
            }
            material = hand.getType();
        } else {
            sender.sendMessage(color("&cUsage: /worth <material>"));
            return true;
        }

        CatalogEntry entry = marketService.entryFor(material);
        if (entry == null) {
            sender.sendMessage(color("&cThe market doesn't trade " + material.name() + "."));
            return true;
        }
        sender.sendMessage(color("&6&l" + material.name()));
        sender.sendMessage(color(String.format(Locale.US, "&aBuy: $%,.2f", pricing.buyPrice(entry))));
        sender.sendMessage(color(String.format(Locale.US, "&6Sell: $%,.2f/each", pricing.sellPrice(entry))));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
