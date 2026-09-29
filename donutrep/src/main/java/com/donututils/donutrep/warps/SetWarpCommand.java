package com.donututils.donutrep.warps;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /setwarp <name> - admin command, creates or updates a warp at your current location. */
public final class SetWarpCommand implements CommandExecutor {

    private final WarpManager warpManager;

    public SetWarpCommand(WarpManager warpManager) {
        this.warpManager = warpManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("warps.admin")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(color("&cUsage: /setwarp <name>"));
            return true;
        }
        warpManager.setWarp(args[0], player.getLocation());
        player.sendMessage(color("&aWarp '" + args[0] + "' set to your current location."));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
