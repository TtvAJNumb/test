package com.donututils.donutrep.social;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Private messages (/msg, /pm) and /reply's "last partner" tracking. Both sides of a conversation
 * are recorded as each other's reply target, matching how every SMP's /r command works. */
public final class MessagingManager {

    private final IgnoreManager ignoreManager;
    private final Map<UUID, UUID> lastPartner = new ConcurrentHashMap<>();

    public MessagingManager(IgnoreManager ignoreManager) {
        this.ignoreManager = ignoreManager;
    }

    /** Returns an error message on failure, or null on success. */
    public String send(Player from, Player to, String message) {
        if (to.getUniqueId().equals(from.getUniqueId())) {
            return "You can't message yourself.";
        }
        if (ignoreManager.isIgnoring(to.getUniqueId(), from.getUniqueId())) {
            return to.getName() + " isn't accepting messages from you.";
        }
        from.sendMessage(color("&7[me -> " + to.getName() + "] &f" + message));
        to.sendMessage(color("&7[" + from.getName() + " -> me] &f" + message));
        lastPartner.put(from.getUniqueId(), to.getUniqueId());
        lastPartner.put(to.getUniqueId(), from.getUniqueId());
        return null;
    }

    public UUID lastPartnerOf(UUID playerId) {
        return lastPartner.get(playerId);
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
