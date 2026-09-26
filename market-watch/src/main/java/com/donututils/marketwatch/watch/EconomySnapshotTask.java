package com.donututils.marketwatch.watch;

import com.donututils.marketwatch.model.EconomySnapshot;
import com.donututils.marketwatch.reflect.UdsBridge;
import com.donututils.marketwatch.storage.EconomySnapshotStore;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

/**
 * Sums every known player's money off UltimateDonutSmp's own money leaderboard and records one
 * snapshot. Deliberately does not touch UltimateDonutSmp's database directly - the leaderboard
 * getters are the safe, intended, thread-tolerant public surface for this.
 */
public final class EconomySnapshotTask implements Runnable {

    private final Plugin plugin;
    private final UdsBridge bridge;
    private final EconomySnapshotStore store;

    public EconomySnapshotTask(Plugin plugin, UdsBridge bridge, EconomySnapshotStore store) {
        this.plugin = plugin;
        this.bridge = bridge;
        this.store = store;
    }

    @Override
    public void run() {
        try {
            double total = bridge.getTotalCirculatingMoney();
            int playerCount = bridge.getKnownPlayerCount();
            store.append(new EconomySnapshot(System.currentTimeMillis(), total, playerCount));
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to take an economy snapshot", ex);
        }
    }
}
