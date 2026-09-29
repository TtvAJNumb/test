package com.donututils.donutrep.help;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * /help: an admin category browser (/help <topic> for any registered subsystem - sus, ah, order,
 * cratebind, and so on) plus a plain player-facing quickstart when a non-staff player runs it with no
 * arguments, teaching the handful of commands a new player actually needs on day one.
 */
public final class HelpCommand implements CommandExecutor {

    private static final List<String> PLAYER_QUICKSTART = List.of(
            "&6&lWelcome! &7Here's how to get started:",
            "&e/shop &7- buy and sell items, and browse the Shard Boutique.",
            "&e/ah &7- the Auction House - list your own items, or buy others'.",
            "&e/pay <player> <amount> &7- send Money to a friend.",
            "&e/sethome &7and &e/home &7- set and teleport back to your base.",
            "&e/rtp &7- random-teleport somewhere new to explore.",
            "&e/chat local &7- switch to local chat if you just want nearby players to see you.",
            "&e/team create <name> &7- form a team with friends.",
            "&e/duel <player> &7or &e/queue &7- challenge someone, or find a random opponent.",
            "&7Run &e/help admin &7for the full admin command list if you're staff."
    );

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (isStaff(sender)) {
                sendTopicList(sender);
            } else {
                for (String line : PLAYER_QUICKSTART) {
                    sender.sendMessage(color(line));
                }
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("admin") || args[0].equalsIgnoreCase("list")) {
            sendTopicList(sender);
            return true;
        }

        String[] lines = HelpTopics.TOPICS.get(args[0].toLowerCase(Locale.ROOT));
        if (lines == null) {
            sender.sendMessage(color("&cNo help topic named '" + args[0] + "'. Run &f/help admin &cfor the full list."));
            return true;
        }
        for (String line : lines) {
            sender.sendMessage(color(line));
        }
        return true;
    }

    private void sendTopicList(CommandSender sender) {
        sender.sendMessage(color("&6&lHelp Topics &7- /help <topic>"));
        sender.sendMessage(color("&e" + String.join("&7, &e", new TreeSet<>(HelpTopics.TOPICS.keySet()))));
    }

    private boolean isStaff(CommandSender sender) {
        return sender.hasPermission("donutrep.help.admin") || sender.isOp();
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
