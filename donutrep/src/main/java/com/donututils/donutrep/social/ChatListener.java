package com.donututils.donutrep.social;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Iterator;
import java.util.function.DoubleSupplier;

/** Enforces the sender's chosen chat channel (see {@link ChatManager}/{@link ChatCommand}) and
 * ignore lists on every chat message: LOCAL trims recipients down to those within
 * local-chat-radius-blocks and same world, and anyone ignoring the sender is dropped from the
 * recipient list regardless of channel. */
public final class ChatListener implements Listener {

    private final ChatManager chatManager;
    private final IgnoreManager ignoreManager;
    private final DoubleSupplier localRadius;

    public ChatListener(ChatManager chatManager, IgnoreManager ignoreManager, DoubleSupplier localRadius) {
        this.chatManager = chatManager;
        this.ignoreManager = ignoreManager;
        this.localRadius = localRadius;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        ChatManager.Channel channel = chatManager.channelOf(sender.getUniqueId());

        if (channel == ChatManager.Channel.LOCAL) {
            double radius = localRadius.getAsDouble();
            event.setFormat(ChatColor.GRAY + "[L] " + ChatColor.RESET + "%1$s" + ChatColor.GRAY + ": " + ChatColor.RESET + "%2$s");
            Iterator<Player> iterator = event.getRecipients().iterator();
            while (iterator.hasNext()) {
                Player recipient = iterator.next();
                if (recipient.equals(sender)) {
                    continue;
                }
                if (!recipient.getWorld().equals(sender.getWorld())
                        || recipient.getLocation().distance(sender.getLocation()) > radius) {
                    iterator.remove();
                }
            }
        }

        event.getRecipients().removeIf(recipient -> !recipient.equals(sender)
                && ignoreManager.isIgnoring(recipient.getUniqueId(), sender.getUniqueId()));
    }
}
