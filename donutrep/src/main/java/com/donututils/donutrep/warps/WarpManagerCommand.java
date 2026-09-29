package com.donututils.donutrep.warps;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;

/** /warpmanager &lt;list|delete <name>&gt; - admin overview of every warp, with coordinates (use
 * /setwarp to create one). */
public final class WarpManagerCommand implements CommandExecutor {

    private final WarpManager warpManager;

    public WarpManagerCommand(WarpManager warpManager) {
        this.warpManager = warpManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("warps.admin")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("delete")) {
            if (args.length < 2) {
                sender.sendMessage(color("&cUsage: /warpmanager delete <name>"));
                return true;
            }
            if (warpManager.deleteWarp(args[1])) {
                sender.sendMessage(color("&aDeleted warp '" + args[1] + "'."));
            } else {
                sender.sendMessage(color("&cNo warp named '" + args[1] + "'."));
            }
            return true;
        }
        List<String> names = warpManager.warpNames();
        if (names.isEmpty()) {
            sender.sendMessage(color("&7No warps have been set yet."));
            return true;
        }
        sender.sendMessage(color("&6&lWarps &7(" + names.size() + ")"));
        for (String name : names) {
            Location location = warpManager.warp(name);
            sender.sendMessage(color(String.format(Locale.US, "&e%s &7- %s (%.0f, %.0f, %.0f)",
                    name, location.getWorld().getName(), location.getX(), location.getY(), location.getZ())));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
