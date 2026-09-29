package com.donututils.donutrep.sell.command;

import com.donututils.donutrep.sell.SellService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /sellhand - sells the item currently in your hand. */
public final class SellHandCommand implements CommandExecutor {

    private final SellService sellService;

    public SellHandCommand(SellService sellService) {
        this.sellService = sellService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        SellService.SellResult result = sellService.sellHand(player);
        player.sendMessage(color((result.success() ? "&a" : "&c") + result.message()));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
