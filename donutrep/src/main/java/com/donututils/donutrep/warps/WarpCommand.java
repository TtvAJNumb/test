package com.donututils.donutrep.warps;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** /warp [name] - teleport to a named warp, or list every warp with no argument. */
public final class WarpCommand implements CommandExecutor {

    private final WarpManager warpManager;

    public WarpCommand(WarpManager warpManager) {
        this.warpManager = warpManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        if (args.length < 1) {
            List<String> names = warpManager.warpNames();
            if (names.isEmpty()) {
                player.sendMessage(color("&7No warps have been set yet."));
            } else {
                player.sendMessage(color("&6&lWarps &7- &f" + String.join("&7, &f", names)));
            }
            return true;
        }
        Location location = warpManager.warp(args[0]);
        if (location == null) {
            player.sendMessage(color("&cNo warp named '" + args[0] + "'."));
            return true;
        }
        player.teleport(location);
        player.sendMessage(color("&aWarped to '" + args[0] + "'."));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
