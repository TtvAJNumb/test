package com.donututils.donutrep.staff;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /fly [player] - toggles flight for the sender, or (with permission) a target player. */
public final class FlyCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("staff.fly.others")) {
                sender.sendMessage(color("&cYou do not have permission to toggle another player's flight."));
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(color("&c" + args[0] + " isn't online."));
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(color("&cUsage: /fly <player>"));
            return true;
        }

        boolean nowFlying = !target.getAllowFlight();
        target.setAllowFlight(nowFlying);
        target.setFlying(nowFlying);
        target.sendMessage(color(nowFlying ? "&aFlight enabled." : "&aFlight disabled."));
        if (sender != target) {
            sender.sendMessage(color("&aSet " + target.getName() + "'s flight to " + nowFlying + "."));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
