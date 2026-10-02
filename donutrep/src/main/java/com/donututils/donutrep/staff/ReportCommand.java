package com.donututils.donutrep.staff;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /report <player> <reason> - alerts online staff about a player, for rule violations that don't
 * need an immediate staff response the way /helpop does. */
public final class ReportCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /report <player> <reason>"));
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(color("&c" + args[0] + " isn't online."));
            return true;
        }
        if (target == player) {
            player.sendMessage(color("&cYou can't report yourself."));
            return true;
        }
        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        String formatted = color("&6&l[Report] &f" + player.getName() + " &7reported &f" + target.getName()
                + " &7- &f" + reason);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("staff.report.receive")) {
                online.sendMessage(formatted);
            }
        }
        player.sendMessage(color("&aYour report against " + target.getName() + " was sent to staff."));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
