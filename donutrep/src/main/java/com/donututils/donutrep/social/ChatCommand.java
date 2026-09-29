package com.donututils.donutrep.social;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/** /chat [global|local] - switches which chat channel your messages go to, or with no argument
 * reports which one you're currently on. */
public final class ChatCommand implements CommandExecutor {

    private final ChatManager chatManager;

    public ChatCommand(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(color("&7You're on the &f" + chatManager.channelOf(player.getUniqueId()) + " &7channel. Usage: /chat <global|local>"));
            return true;
        }
        ChatManager.Channel channel;
        try {
            channel = ChatManager.Channel.valueOf(args[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            player.sendMessage(color("&cUsage: /chat <global|local>"));
            return true;
        }
        chatManager.setChannel(player.getUniqueId(), channel);
        player.sendMessage(color("&aSwitched to the &f" + channel + " &achat channel."));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
