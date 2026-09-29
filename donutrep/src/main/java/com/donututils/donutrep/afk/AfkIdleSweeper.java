package com.donututils.donutrep.afk;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.function.LongSupplier;

/** Runs periodically; anyone idle past afk-timeout-seconds who isn't already AFK gets marked AFK
 * automatically. */
public final class AfkIdleSweeper implements Runnable {

    private final AfkManager afkManager;
    private final LongSupplier timeoutMillis;

    public AfkIdleSweeper(AfkManager afkManager, LongSupplier timeoutMillis) {
        this.afkManager = afkManager;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public void run() {
        long timeout = timeoutMillis.getAsLong();
        if (timeout <= 0) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!afkManager.isAfk(player.getUniqueId()) && afkManager.idleMillis(player.getUniqueId()) >= timeout) {
                afkManager.setAfk(player, true);
            }
        }
    }
}
