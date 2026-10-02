package com.donututils.donutrep.staff;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /feed [player] - fully restores food level. */
public final class FeedCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("staff.feed.others")) {
                sender.sendMessage(color("&cYou do not have permission to feed another player."));
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
            sender.sendMessage(color("&cUsage: /feed <player>"));
            return true;
        }

        target.setFoodLevel(20);
        target.sendMessage(color("&aYou've been fed."));
        if (sender != target) {
            sender.sendMessage(color("&aFed " + target.getName() + "."));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
