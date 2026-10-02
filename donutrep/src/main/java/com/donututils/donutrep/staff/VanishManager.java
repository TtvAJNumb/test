package com.donututils.donutrep.staff;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks vanished players and keeps everyone's visibility in sync - vanished players are hidden from
 * everyone except other players holding staff.vanish.see, and a vanished player re-syncs for whoever
 * joins afterward. */
public final class VanishManager {

    private final Plugin plugin;
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();

    public VanishManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isVanished(UUID playerId) {
        return vanished.contains(playerId);
    }

    public boolean toggle(Player player) {
        if (vanished.remove(player.getUniqueId())) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.showPlayer(plugin, player);
            }
            return false;
        }
        vanished.add(player.getUniqueId());
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online != player && !online.hasPermission("staff.vanish.see")) {
                online.hidePlayer(plugin, player);
            }
        }
        return true;
    }

    /** Called on join: hides every vanished player from the joiner unless they can see through it, and
     * hides the joiner from anyone who shouldn't see them if the joiner is itself still flagged vanished
     * (e.g. re-logging while in vanish). */
    public void syncOnJoin(Player joined) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == joined) {
                continue;
            }
            if (isVanished(online.getUniqueId()) && !joined.hasPermission("staff.vanish.see")) {
                joined.hidePlayer(plugin, online);
            }
            if (isVanished(joined.getUniqueId()) && !online.hasPermission("staff.vanish.see")) {
                online.hidePlayer(plugin, joined);
            }
        }
    }
}
