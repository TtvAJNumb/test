package com.donututils.crateodds.listener;

import com.donututils.crateodds.history.HistoryStore;
import com.donututils.crateodds.model.HistoryEntry;
import com.donututils.crateodds.model.RewardOdds;
import com.donututils.crateodds.reflect.UdsBridge;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Detects when a player opens a crate through one of CrateBindAddon's bound blocks, then polls
 * UltimateDonutSmp's in-memory crate session until a reward has been rolled, and records it.
 * <p>
 * UDS has no event or persisted log for "player opened crate X and got reward Y" - the roll
 * result only ever exists as {@code CrateManager.CrateOpenSession#selectedReward()} for the
 * few seconds between the roll and the claim. Polling that session at short intervals is the
 * only way to observe it without patching UDS itself.
 */
public final class OpenWatcher implements Listener {

    private final Plugin plugin;
    private final UdsBridge bridge;
    private final HistoryStore historyStore;
    private final int pollIntervalTicks;
    private final int timeoutTicks;

    private final Map<UUID, BukkitTask> watching = new ConcurrentHashMap<>();

    public OpenWatcher(Plugin plugin, UdsBridge bridge, HistoryStore historyStore,
                        int pollIntervalTicks, int timeoutTicks) {
        this.plugin = plugin;
        this.bridge = bridge;
        this.historyStore = historyStore;
        this.pollIntervalTicks = Math.max(1, pollIntervalTicks);
        this.timeoutTicks = Math.max(this.pollIntervalTicks, timeoutTicks);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        String crateId;
        try {
            crateId = bridge.boundCrateId(block);
        } catch (RuntimeException ex) {
            return;
        }
        if (crateId == null) {
            return;
        }

        startWatching(event.getPlayer().getUniqueId(), crateId);
    }

    private void startWatching(UUID uuid, String crateId) {
        if (watching.containsKey(uuid)) {
            return;
        }

        int[] ticksWaited = {0};
        BukkitTask[] selfRef = new BukkitTask[1];
        selfRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ticksWaited[0] += pollIntervalTicks;
            boolean done = pollOnce(uuid, crateId);
            if (done || ticksWaited[0] >= timeoutTicks) {
                stopWatching(uuid, selfRef[0]);
            }
        }, pollIntervalTicks, pollIntervalTicks);

        watching.put(uuid, selfRef[0]);
    }

    /** @return true if a result was found (or the session vanished) and polling should stop. */
    private boolean pollOnce(UUID uuid, String crateId) {
        Object session;
        try {
            session = bridge.getSessionRaw(uuid);
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to read crate session for " + uuid, ex);
            return true;
        }

        if (session == null) {
            // Session already cleared (claimed very quickly, or the player backed out) - nothing to record.
            return true;
        }

        try {
            Object reward = bridge.getSelectedReward(session);
            if (reward == null) {
                return false; // still spinning / not selected yet, keep polling
            }
            recordOpen(uuid, crateId, reward);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to read crate reward for " + uuid, ex);
        }
        return true;
    }

    private void recordOpen(UUID uuid, String crateId, Object rewardObj) {
        try {
            Object crateDefinition = bridge.getCrateDefinitionRaw(crateId);
            RewardOdds resolved = bridge.toRewardOdds(crateDefinition, rewardObj);

            HistoryEntry entry = new HistoryEntry(
                    System.currentTimeMillis(),
                    crateId,
                    resolved.rewardId(),
                    resolved.displayName(),
                    resolved.grantSummary(),
                    resolved.percent()
            );
            historyStore.addEntry(uuid, entry);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to record crate open for " + uuid, ex);
        }
    }

    private void stopWatching(UUID uuid, BukkitTask task) {
        watching.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    public void shutdown() {
        watching.values().forEach(BukkitTask::cancel);
        watching.clear();
    }
}
