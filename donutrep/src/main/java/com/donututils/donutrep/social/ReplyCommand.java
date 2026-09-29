package com.donututils.donutrep.social;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/** /reply <message> - sends to whoever last messaged you (or you last messaged). */
public final class ReplyCommand implements CommandExecutor {

    private final MessagingManager messagingManager;

    public ReplyCommand(MessagingManager messagingManager) {
        this.messagingManager = messagingManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(color("&cUsage: /reply <message>"));
            return true;
        }
        UUID partnerId = messagingManager.lastPartnerOf(player.getUniqueId());
        if (partnerId == null) {
            player.sendMessage(color("&cNo one to reply to yet."));
            return true;
        }
        Player target = Bukkit.getPlayer(partnerId);
        if (target == null || !target.isOnline()) {
            player.sendMessage(color("&cThey're no longer online."));
            return true;
        }
        String message = String.join(" ", args);
        String error = messagingManager.send(player, target, message);
        if (error != null) {
            player.sendMessage(color("&c" + error));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
