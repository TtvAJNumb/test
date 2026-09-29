package com.donututils.donutrep.pvp;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /leave - forfeits your active duel, or leaves the matchmaking queue if you're only waiting. */
public final class LeaveCommand implements CommandExecutor {

    private final DuelManager duelManager;
    private final DuelQueueManager queueManager;

    public LeaveCommand(DuelManager duelManager, DuelQueueManager queueManager) {
        this.duelManager = duelManager;
        this.queueManager = queueManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        if (duelManager.isInDuel(player.getUniqueId())) {
            duelManager.end(player.getUniqueId(), "forfeited");
            return true;
        }
        if (queueManager.leave(player.getUniqueId())) {
            player.sendMessage(color("&7Left the duel queue."));
        } else {
            player.sendMessage(color("&cYou're not in a duel or queue."));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
