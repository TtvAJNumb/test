package com.donututils.donutrep.social;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /unignore <player> - undoes a previous /ignore. */
public final class UnignoreCommand implements CommandExecutor {

    private final IgnoreManager ignoreManager;

    public UnignoreCommand(IgnoreManager ignoreManager) {
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
            player.sendMessage(color("&cUsage: /unignore <player>"));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (ignoreManager.unignore(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(color("&aNo longer ignoring " + args[0] + "."));
        } else {
            player.sendMessage(color("&cYou weren't ignoring " + args[0] + "."));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
