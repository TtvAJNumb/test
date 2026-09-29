package com.donututils.donutrep.market.command;

import com.donututils.donutrep.market.service.MarketService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/** /sellmulti <material> <amount> - sells a specific amount of a specific material from your
 * inventory, regardless of what's in hand. */
public final class SellMultiCommand implements CommandExecutor {

    private final MarketService marketService;

    public SellMultiCommand(MarketService marketService) {
        this.marketService = marketService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /sellmulti <material> <amount>"));
            return true;
        }
        Material material;
        try {
            material = Material.valueOf(args[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            player.sendMessage(color("&cUnknown material: " + args[0]));
            return true;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage(color("&cInvalid amount."));
            return true;
        }
        MarketService.TradeResult result = marketService.sell(player, material, amount);
        player.sendMessage(color((result.success() ? "&a" : "&c") + result.message()));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
