package com.donututils.donutrep.pvp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Simple 1v1 duels: no arena teleport, item wagering, or health/inventory snapshot restore - both
 * players just fight where they stand, and whoever dies (or forfeits with /leave) loses. Vanilla
 * respawn handles the loser afterward. A bounded v1, not the original UltimateDonutSmp's full duel
 * economy/arena system. */
public final class DuelManager {

    private record Challenge(UUID challenger, long expiresAtMillis) {
    }

    private final Map<UUID, Challenge> pendingChallenges = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> activeDuels = new ConcurrentHashMap<>();
    private final long challengeTimeoutMillis;

    public DuelManager(long challengeTimeoutMillis) {
        this.challengeTimeoutMillis = challengeTimeoutMillis;
    }

    public boolean isInDuel(UUID playerId) {
        return activeDuels.containsKey(playerId);
    }

    public UUID opponentOf(UUID playerId) {
        return activeDuels.get(playerId);
    }

    public String challenge(Player challenger, Player target) {
        if (isInDuel(challenger.getUniqueId()) || isInDuel(target.getUniqueId())) {
            return "One of you is already in a duel.";
        }
        pendingChallenges.put(target.getUniqueId(), new Challenge(challenger.getUniqueId(), System.currentTimeMillis() + challengeTimeoutMillis));
        target.sendMessage(color("&e" + challenger.getName() + " challenged you to a duel! /duel accept or /duel decline"));
        challenger.sendMessage(color("&aChallenge sent to " + target.getName() + "."));
        return null;
    }

    public String accept(Player target) {
        Challenge challenge = pendingChallenges.remove(target.getUniqueId());
        if (challenge == null || challenge.expiresAtMillis() < System.currentTimeMillis()) {
            return "You have no pending challenge.";
        }
        Player challenger = Bukkit.getPlayer(challenge.challenger());
        if (challenger == null || !challenger.isOnline()) {
            return "That player is no longer online.";
        }
        activeDuels.put(target.getUniqueId(), challenger.getUniqueId());
        activeDuels.put(challenger.getUniqueId(), target.getUniqueId());
        target.sendMessage(color("&aDuel with " + challenger.getName() + " started - fight!"));
        challenger.sendMessage(color("&a" + target.getName() + " accepted - fight!"));
        return null;
    }

    public String decline(Player target) {
        Challenge challenge = pendingChallenges.remove(target.getUniqueId());
        if (challenge == null) {
            return "You have no pending challenge.";
        }
        Player challenger = Bukkit.getPlayer(challenge.challenger());
        if (challenger != null) {
            challenger.sendMessage(color("&c" + target.getName() + " declined your duel."));
        }
        return null;
    }

    /** Ends a duel (death or /leave forfeit) and returns the opponent, or null if not in one. */
    public UUID end(UUID playerId, String reason) {
        UUID opponentId = activeDuels.remove(playerId);
        if (opponentId == null) {
            return null;
        }
        activeDuels.remove(opponentId);
        Player loser = Bukkit.getPlayer(playerId);
        Player winner = Bukkit.getPlayer(opponentId);
        String loserName = loser != null ? loser.getName() : "Someone";
        String winnerName = winner != null ? winner.getName() : "their opponent";
        String message = color("&6" + winnerName + " &7won the duel against &6" + loserName + " &7(" + reason + ").");
        if (winner != null) {
            winner.sendMessage(message);
        }
        if (loser != null) {
            loser.sendMessage(message);
        }
        return opponentId;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
