package com.donututils.donutrep.social;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

/** /msg and /pm both run this - a private message to another online player. */
public final class MsgCommand implements CommandExecutor {

    private final MessagingManager messagingManager;

    public MsgCommand(MessagingManager messagingManager) {
        this.messagingManager = messagingManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /" + label + " <player> <message>"));
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(color("&c" + args[0] + " isn't online."));
            return true;
        }
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
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
