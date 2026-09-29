package com.donututils.donutrep.pvp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /duel <player> - challenge someone to a 1v1; /duel accept or /duel decline answers a pending
 * challenge sent to you. */
public final class DuelCommand implements CommandExecutor {

    private final DuelManager duelManager;

    public DuelCommand(DuelManager duelManager) {
        this.duelManager = duelManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(color("&cUsage: /duel <player|accept|decline>"));
            return true;
        }
        if (args[0].equalsIgnoreCase("accept")) {
            String error = duelManager.accept(player);
            if (error != null) {
                player.sendMessage(color("&c" + error));
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("decline")) {
            String error = duelManager.decline(player);
            if (error != null) {
                player.sendMessage(color("&c" + error));
            }
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(color("&c" + args[0] + " isn't online."));
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(color("&cYou can't duel yourself."));
            return true;
        }
        String error = duelManager.challenge(player, target);
        if (error != null) {
            player.sendMessage(color("&c" + error));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
