package com.donututils.donutrep.sell.command;

import com.donututils.donutrep.sell.SellService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /sellall - sells every sellable item in your inventory. */
public final class SellAllCommand implements CommandExecutor {

    private final SellService sellService;

    public SellAllCommand(SellService sellService) {
        this.sellService = sellService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        SellService.SellResult result = sellService.sellAllInventory(player);
        player.sendMessage(color((result.success() ? "&a" : "&c") + result.message()));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
