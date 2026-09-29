package com.donututils.donutrep.social;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/** Enforces global chat mute/slow-mode (see {@link GlobalChatManager}/{@link ChatCommand}) and
 * ignore lists on every chat message. */
public final class ChatListener implements Listener {

    private final GlobalChatManager globalChatManager;
    private final IgnoreManager ignoreManager;

    public ChatListener(GlobalChatManager globalChatManager, IgnoreManager ignoreManager) {
        this.globalChatManager = globalChatManager;
        this.ignoreManager = ignoreManager;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();

        if (globalChatManager.isMuted() && !sender.hasPermission("social.chat.bypass")) {
            event.setCancelled(true);
            sender.sendMessage(color("&cChat is currently muted by staff."));
            return;
        }

        if (!sender.hasPermission("social.chat.bypass") && !globalChatManager.checkAndRecordDelay(sender.getUniqueId())) {
            event.setCancelled(true);
            sender.sendMessage(color("&cYou're chatting too fast - slow down."));
            return;
        }

        event.getRecipients().removeIf(recipient -> !recipient.equals(sender)
                && ignoreManager.isIgnoring(recipient.getUniqueId(), sender.getUniqueId()));
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
