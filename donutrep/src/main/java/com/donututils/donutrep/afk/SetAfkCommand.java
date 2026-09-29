package com.donututils.donutrep.afk;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/** /setafk <player> [on|off] - staff command to force another player's AFK status (e.g. clearing a
 * stuck AFK flag, or marking someone AFK for a moderation reason). */
public final class SetAfkCommand implements CommandExecutor {

    private final AfkManager afkManager;

    public SetAfkCommand(AfkManager afkManager) {
        this.afkManager = afkManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("afk.admin")) {
            sender.sendMessage(color("&cYou do not have permission to do that."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(color("&cUsage: /setafk <player> [on|off]"));
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(color("&c" + args[0] + " isn't online."));
            return true;
        }
        boolean value = args.length < 2 || !args[1].equalsIgnoreCase("off");
        if (args.length >= 2 && !args[1].toLowerCase(Locale.ROOT).matches("on|off")) {
            sender.sendMessage(color("&cUsage: /setafk <player> [on|off]"));
            return true;
        }
        afkManager.setAfk(target, value);
        sender.sendMessage(color("&aSet " + target.getName() + "'s AFK status to " + value + "."));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
