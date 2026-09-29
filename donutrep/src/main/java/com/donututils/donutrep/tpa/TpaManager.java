package com.donututils.donutrep.tpa;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Player-to-player teleport requests: /tpa (come to me... no - I request to go to you),
 * /tpahere (I request you come to me), with /tpaccept, /tpadeny, /tpacancel, and per-type
 * auto-accept toggles (/tpauto, /tpahereauto). */
public final class TpaManager {

    public enum Type { TPA, TPA_HERE }

    private record PendingRequest(UUID requesterId, String requesterName, Type type, long expiresAtMillis) {
    }

    private final Map<UUID, List<PendingRequest>> incoming = new ConcurrentHashMap<>();
    private final Set<UUID> autoAcceptTpa = ConcurrentHashMap.newKeySet();
    private final Set<UUID> autoAcceptTpaHere = ConcurrentHashMap.newKeySet();
    private final long requestTimeoutMillis;

    public TpaManager(long requestTimeoutMillis) {
        this.requestTimeoutMillis = requestTimeoutMillis;
    }

    /** Returns an error message, or null if the request was sent (and possibly auto-accepted). */
    public String request(Player requester, Player target, Type type) {
        if (requester.getUniqueId().equals(target.getUniqueId())) {
            return "You can't send a teleport request to yourself.";
        }
        boolean autoAccept = type == Type.TPA ? autoAcceptTpa.contains(target.getUniqueId()) : autoAcceptTpaHere.contains(target.getUniqueId());
        if (autoAccept) {
            execute(requester, target, type);
            requester.sendMessage("§a" + target.getName() + " has auto-accept on - teleported.");
            return null;
        }
        incoming.computeIfAbsent(target.getUniqueId(), id -> new ArrayList<>())
                .add(new PendingRequest(requester.getUniqueId(), requester.getName(), type, System.currentTimeMillis() + requestTimeoutMillis));
        String verb = type == Type.TPA ? "teleport to you" : "have you teleport to them";
        target.sendMessage("§e" + requester.getName() + " wants to " + verb + ". /tpaccept or /tpadeny (expires in "
                + (requestTimeoutMillis / 1000) + "s)");
        requester.sendMessage("§aRequest sent to " + target.getName() + ".");
        return null;
    }

    /** Accepts the sole pending request for this target, or the one from requesterName if given.
     * Returns an error, or null on success. */
    public String accept(Player target, String requesterName, java.util.function.Function<UUID, Player> resolvePlayer) {
        PendingRequest request = takeRequest(target.getUniqueId(), requesterName);
        if (request == null) {
            return requesterName != null ? "No pending request from " + requesterName + "." : "You have no pending requests.";
        }
        Player requester = resolvePlayer.apply(request.requesterId());
        if (requester == null || !requester.isOnline()) {
            return "That player is no longer online.";
        }
        execute(requester, target, request.type());
        return null;
    }

    public String deny(Player target, String requesterName, java.util.function.Function<UUID, Player> resolvePlayer) {
        PendingRequest request = takeRequest(target.getUniqueId(), requesterName);
        if (request == null) {
            return requesterName != null ? "No pending request from " + requesterName + "." : "You have no pending requests.";
        }
        Player requester = resolvePlayer.apply(request.requesterId());
        if (requester != null) {
            requester.sendMessage("§c" + target.getName() + " denied your teleport request.");
        }
        return null;
    }

    /** Cancels every pending request this player has sent out. Returns how many were cancelled. */
    public int cancel(UUID requesterId) {
        int count = 0;
        for (List<PendingRequest> requests : incoming.values()) {
            count += requests.size();
            requests.removeIf(r -> r.requesterId().equals(requesterId));
            count -= requests.size();
        }
        return count;
    }

    public boolean toggleAutoTpa(UUID playerId) {
        return toggle(autoAcceptTpa, playerId);
    }

    public boolean toggleAutoTpaHere(UUID playerId) {
        return toggle(autoAcceptTpaHere, playerId);
    }

    private boolean toggle(Set<UUID> set, UUID playerId) {
        if (!set.add(playerId)) {
            set.remove(playerId);
            return false;
        }
        return true;
    }

    private PendingRequest takeRequest(UUID targetId, String requesterName) {
        List<PendingRequest> requests = incoming.get(targetId);
        if (requests == null) {
            return null;
        }
        requests.removeIf(r -> r.expiresAtMillis() < System.currentTimeMillis());
        if (requests.isEmpty()) {
            return null;
        }
        PendingRequest match;
        if (requesterName != null) {
            match = requests.stream().filter(r -> r.requesterName().equalsIgnoreCase(requesterName)).findFirst().orElse(null);
        } else if (requests.size() == 1) {
            match = requests.get(0);
        } else {
            return null;
        }
        if (match != null) {
            requests.remove(match);
        }
        return match;
    }

    private void execute(Player requester, Player target, Type type) {
        if (type == Type.TPA) {
            requester.teleport(target.getLocation());
        } else {
            target.teleport(requester.getLocation());
        }
    }
}
