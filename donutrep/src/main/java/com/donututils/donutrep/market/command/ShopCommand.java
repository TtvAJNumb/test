package com.donututils.donutrep.market.command;

import com.donututils.donutrep.market.gui.MarketGuiService;
import com.donututils.donutrep.market.service.MarketService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ShopCommand implements CommandExecutor {

    private final MarketGuiService guiService;
    private final MarketService marketService;

    public ShopCommand(MarketGuiService guiService, MarketService marketService) {
        this.guiService = guiService;
        this.marketService = marketService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (args.length == 0) {
            guiService.openRoot(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("sell") && args.length >= 2 && args[1].equalsIgnoreCase("hand")) {
            MarketService.TradeResult result = marketService.sellHand(player);
            player.sendMessage(color((result.success() ? "&a" : "&c") + result.message()));
            return true;
        }
        player.sendMessage(color("&cUsage: /shop [sell hand]"));
        return true;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(color("&cOnly players can do that."));
        return null;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
