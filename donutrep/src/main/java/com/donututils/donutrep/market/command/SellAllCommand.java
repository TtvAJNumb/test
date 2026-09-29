package com.donututils.donutrep.market.command;

import com.donututils.donutrep.market.service.MarketService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /sellall - sells every sellable item in your inventory at once. */
public final class SellAllCommand implements CommandExecutor {

    private final MarketService marketService;

    public SellAllCommand(MarketService marketService) {
        this.marketService = marketService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        MarketService.TradeResult result = marketService.sellAllInventory(player);
        player.sendMessage(color((result.success() ? "&a" : "&c") + result.message()));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
