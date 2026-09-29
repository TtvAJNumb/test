package com.donututils.donutrep.social;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Locale;

/** /chat &lt;help|mute|unmute|delay|clear&gt; - staff administration of global chat, matching real
 * UDS's /chat (not a per-player channel switch - there's no such command in the real plugin). */
public final class ChatCommand implements CommandExecutor {

    private final GlobalChatManager globalChatManager;

    public ChatCommand(GlobalChatManager globalChatManager) {
        this.globalChatManager = globalChatManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("social.command.chat")) {
            sender.sendMessage(color("&cYou do not have permission to use /chat."));
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> sendHelp(sender);
            case "mute" -> {
                globalChatManager.setMuted(true);
                Bukkit.broadcastMessage(color("&cGlobal chat has been muted."));
            }
            case "unmute" -> {
                globalChatManager.setMuted(false);
                Bukkit.broadcastMessage(color("&aGlobal chat has been unmuted."));
            }
            case "delay" -> {
                if (args.length < 2) {
                    sender.sendMessage(color("&cUsage: /chat delay <seconds>"));
                    return true;
                }
                int seconds;
                try {
                    seconds = Integer.parseInt(args[1]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(color("&cInvalid number of seconds."));
                    return true;
                }
                globalChatManager.setDelaySeconds(seconds);
                sender.sendMessage(color(seconds <= 0 ? "&aChat delay disabled." : "&aChat delay set to " + seconds + " second(s)."));
            }
            case "clear" -> {
                for (int i = 0; i < 100; i++) {
                    Bukkit.broadcastMessage("");
                }
                Bukkit.broadcastMessage(color("&7Chat cleared by " + sender.getName() + "."));
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(color("&6&lChat &7- /chat <help|mute|unmute|delay <seconds>|clear>"));
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
