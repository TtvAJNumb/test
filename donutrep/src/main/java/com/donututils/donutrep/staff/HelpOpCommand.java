package com.donututils.donutrep.staff;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /helpop <message> - sends a message to every online staff member (anyone with staff.helpop.receive),
 * for a player who needs help without using public chat. */
public final class HelpOpCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(color("&cUsage: /helpop <message>"));
            return true;
        }
        String message = String.join(" ", args);
        String formatted = color("&c&l[HelpOp] &f" + player.getName() + "&7: &f" + message);
        boolean reached = false;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("staff.helpop.receive")) {
                online.sendMessage(formatted);
                reached = true;
            }
        }
        player.sendMessage(color("&aYour message was sent to staff."));
        if (!reached) {
            player.sendMessage(color("&7No staff are online right now - try again later."));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
