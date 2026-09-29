package com.donututils.donutrep.afk;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Who's AFK right now and when each online player was last active - purely a session state, same
 * as chat channel, since it has no meaning after a restart/relog. Manual AFK (via /afk or a staff
 * /setafk) is "sticky" and only clears on real activity or another manual toggle; the automatic
 * idle-timeout AFK set by the background sweep behaves the same way once applied. */
public final class AfkManager {

    private final Set<UUID> afk = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> lastActivityMillis = new ConcurrentHashMap<>();

    public boolean isAfk(UUID playerId) {
        return afk.contains(playerId);
    }

    public void recordActivity(UUID playerId) {
        lastActivityMillis.put(playerId, System.currentTimeMillis());
        if (afk.remove(playerId)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                Bukkit.broadcastMessage(color("&e" + player.getName() + " is no longer AFK."));
            }
        }
    }

    public long idleMillis(UUID playerId) {
        Long last = lastActivityMillis.get(playerId);
        return last == null ? 0 : System.currentTimeMillis() - last;
    }

    /** Manual toggle (/afk, or a staff /setafk). Returns the new state. */
    public boolean toggle(Player player) {
        return setAfk(player, !isAfk(player.getUniqueId()));
    }

    public boolean setAfk(Player player, boolean value) {
        UUID playerId = player.getUniqueId();
        if (value) {
            if (afk.add(playerId)) {
                Bukkit.broadcastMessage(color("&e" + player.getName() + " is now AFK."));
            }
        } else {
            lastActivityMillis.put(playerId, System.currentTimeMillis());
            if (afk.remove(playerId)) {
                Bukkit.broadcastMessage(color("&e" + player.getName() + " is no longer AFK."));
            }
        }
        return value;
    }

    public void forgetPlayer(UUID playerId) {
        afk.remove(playerId);
        lastActivityMillis.remove(playerId);
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
