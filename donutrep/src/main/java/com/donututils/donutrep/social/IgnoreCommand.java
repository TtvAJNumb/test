package com.donututils.donutrep.social;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /ignore <player> - stops receiving their private messages and chat lines. */
public final class IgnoreCommand implements CommandExecutor {

    private final IgnoreManager ignoreManager;

    public IgnoreCommand(IgnoreManager ignoreManager) {
        this.ignoreManager = ignoreManager;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(color("&cUsage: /ignore <player>"));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(color("&cYou can't ignore yourself."));
            return true;
        }
        if (ignoreManager.ignore(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(color("&aIgnoring " + args[0] + "."));
        } else {
            player.sendMessage(color("&cYou're already ignoring " + args[0] + "."));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
