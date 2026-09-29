package com.donututils.donutrep.travel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/** /findplayer <player> - staff command, shows a player's current world and coordinates without
 * teleporting anyone. */
public final class FindPlayerCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("travel.admin")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(color("&cUsage: /findplayer <player>"));
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(color("&c" + args[0] + " isn't online."));
            return true;
        }
        Location location = target.getLocation();
        sender.sendMessage(color(String.format(Locale.US, "&e%s &7is in &f%s &7at &f%.0f, %.0f, %.0f",
                target.getName(), location.getWorld().getName(), location.getX(), location.getY(), location.getZ())));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
