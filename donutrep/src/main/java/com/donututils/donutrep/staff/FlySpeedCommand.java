package com.donututils.donutrep.staff;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /flyspeed <0-10> [player] - sets flight speed (0-10 mapped to Bukkit's -1.0 to 1.0 range). */
public final class FlySpeedCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(color("&cUsage: /flyspeed <0-10> [player]"));
            return true;
        }
        int level;
        try {
            level = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(color("&cInvalid speed - use a number from 0 to 10."));
            return true;
        }
        if (level < 0 || level > 10) {
            sender.sendMessage(color("&cSpeed must be between 0 and 10."));
            return true;
        }

        Player target;
        if (args.length >= 2) {
            if (!sender.hasPermission("staff.fly.others")) {
                sender.sendMessage(color("&cYou do not have permission to set another player's fly speed."));
                return true;
            }
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(color("&c" + args[1] + " isn't online."));
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(color("&cUsage: /flyspeed <0-10> <player>"));
            return true;
        }

        target.setFlySpeed(level / 10.0f);
        target.sendMessage(color("&aFly speed set to " + level + "/10."));
        if (sender != target) {
            sender.sendMessage(color("&aSet " + target.getName() + "'s fly speed to " + level + "/10."));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
