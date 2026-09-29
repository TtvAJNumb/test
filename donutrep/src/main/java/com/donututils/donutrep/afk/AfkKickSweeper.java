package com.donututils.donutrep.afk;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

/** Optional AFK auto-kick: disconnects a player once they've been AFK past kick-after-seconds.
 * Disabled by default (afk.kick.enabled: false in config.yml) - when disabled, /afk only ever
 * tags a player, never disconnects them. */
public final class AfkKickSweeper implements Runnable {

    private final AfkManager afkManager;
    private final BooleanSupplier enabledSupplier;
    private final LongSupplier kickAfterMillis;

    public AfkKickSweeper(AfkManager afkManager, BooleanSupplier enabledSupplier, LongSupplier kickAfterMillis) {
        this.afkManager = afkManager;
        this.enabledSupplier = enabledSupplier;
        this.kickAfterMillis = kickAfterMillis;
    }

    @Override
    public void run() {
        if (!enabledSupplier.getAsBoolean()) {
            return;
        }
        long threshold = kickAfterMillis.getAsLong();
        if (threshold <= 0) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (afkManager.isAfk(player.getUniqueId()) && afkManager.idleMillis(player.getUniqueId()) >= threshold) {
                player.kickPlayer("You were kicked for being AFK too long.");
            }
        }
    }
}
