package com.donututils.donutrep.travel;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /rtpq - joins the random-teleport queue, processed one player at a time at a steady pace instead
 * of piling more concurrent searches onto the server. */
public final class RtpqCommand implements CommandExecutor {

    private final RtpQueueService queueService;

    public RtpqCommand(RtpQueueService queueService) {
        this.queueService = queueService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        if (!queueService.enqueue(player)) {
            player.sendMessage(color("&cYou're already in the queue."));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
