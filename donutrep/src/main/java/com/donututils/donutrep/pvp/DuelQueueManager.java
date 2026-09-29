package com.donututils.donutrep.pvp;

import com.donututils.donutrep.afk.AfkManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.UUID;

/** Random-opponent matchmaking for /queue: the moment two players are waiting, they're paired into a
 * duel automatically (via DuelManager, skipping the challenge/accept handshake since both sides
 * already opted in by queueing). AFK players are never paired (and get dropped from the queue if
 * they go AFK while waiting), so a macro can't sit in queue collecting free duel outcomes. */
public final class DuelQueueManager {

    private final DuelManager duelManager;
    private final AfkManager afkManager;
    private final LinkedHashSet<UUID> waiting = new LinkedHashSet<>();

    public DuelQueueManager(DuelManager duelManager, AfkManager afkManager) {
        this.duelManager = duelManager;
        this.afkManager = afkManager;
    }

    public boolean isQueued(UUID playerId) {
        return waiting.contains(playerId);
    }

    public String join(Player player) {
        if (duelManager.isInDuel(player.getUniqueId())) {
            return "You're already in a duel.";
        }
        if (afkManager.isAfk(player.getUniqueId())) {
            return "You can't queue while AFK.";
        }
        if (!waiting.add(player.getUniqueId())) {
            return "You're already queued.";
        }
        player.sendMessage(color("&7Queued for a random duel opponent..."));
        tryPair();
        return null;
    }

    public boolean leave(UUID playerId) {
        return waiting.remove(playerId);
    }

    private void tryPair() {
        waiting.removeIf(afkManager::isAfk);
        if (waiting.size() < 2) {
            return;
        }
        Iterator<UUID> iterator = waiting.iterator();
        UUID firstId = iterator.next();
        iterator.remove();
        UUID secondId = iterator.next();
        iterator.remove();
        Player first = Bukkit.getPlayer(firstId);
        Player second = Bukkit.getPlayer(secondId);
        if (first == null || !first.isOnline()) {
            if (second != null) {
                waiting.add(secondId);
            }
            return;
        }
        if (second == null || !second.isOnline()) {
            waiting.add(firstId);
            return;
        }
        duelManager.challenge(first, second);
        duelManager.accept(second);
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
