package com.donututils.donutrep.travel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /teleport <player> [target] - admin command: teleports player to target, or to you if no target
 * is given. */
public final class TeleportCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("travel.admin")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(color("&cUsage: /teleport <player> [target]"));
            return true;
        }
        Player player = Bukkit.getPlayer(args[0]);
        if (player == null) {
            sender.sendMessage(color("&c" + args[0] + " isn't online."));
            return true;
        }
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(color("&c" + args[1] + " isn't online."));
                return true;
            }
        } else if (sender instanceof Player senderPlayer) {
            target = senderPlayer;
        } else {
            sender.sendMessage(color("&cUsage: /teleport <player> <target> (a target is required from console)."));
            return true;
        }
        player.teleport(target.getLocation());
        sender.sendMessage(color("&aTeleported " + player.getName() + " to " + target.getName() + "."));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
