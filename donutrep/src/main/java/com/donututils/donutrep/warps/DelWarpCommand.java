package com.donututils.donutrep.warps;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** /delwarp <name> - admin command, deletes a warp. */
public final class DelWarpCommand implements CommandExecutor {

    private final WarpManager warpManager;

    public DelWarpCommand(WarpManager warpManager) {
        this.warpManager = warpManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("warps.admin")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(color("&cUsage: /delwarp <name>"));
            return true;
        }
        if (warpManager.deleteWarp(args[0])) {
            sender.sendMessage(color("&aDeleted warp '" + args[0] + "'."));
        } else {
            sender.sendMessage(color("&cNo warp named '" + args[0] + "'."));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
