package com.donututils.donutrep.pvp;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /queue - joins the random-opponent duel matchmaking queue. */
public final class QueueCommand implements CommandExecutor {

    private final DuelQueueManager queueManager;

    public QueueCommand(DuelQueueManager queueManager) {
        this.queueManager = queueManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        String error = queueManager.join(player);
        if (error != null) {
            player.sendMessage(color("&c" + error));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
