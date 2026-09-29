package com.donututils.donutrep.travel;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.function.Supplier;

/** Backs /rtpq: when the immediate concurrent-search limit is already hit, a player queues up here
 * instead of piling another search onto the server, and gets processed one at a time on a slower,
 * fixed pace. */
public final class RtpQueueService {

    private final Plugin plugin;
    private final RtpManager rtpManager;
    private final Supplier<RtpConfig> configSupplier;
    private final LinkedHashSet<UUID> queue = new LinkedHashSet<>();
    private BukkitTask task;

    public RtpQueueService(Plugin plugin, RtpManager rtpManager, Supplier<RtpConfig> configSupplier) {
        this.plugin = plugin;
        this.rtpManager = rtpManager;
        this.configSupplier = configSupplier;
    }

    public void start() {
        stop();
        long intervalTicks = Math.max(20L, configSupplier.get().queueIntervalSeconds() * 20L);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::processNext, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public boolean enqueue(Player player) {
        if (!queue.add(player.getUniqueId())) {
            return false;
        }
        player.sendMessage(color("&7You're #" + queue.size() + " in the random-teleport queue."));
        return true;
    }

    private void processNext() {
        if (queue.isEmpty()) {
            return;
        }
        UUID next = queue.iterator().next();
        queue.remove(next);
        org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(next);
        if (player == null || !player.isOnline()) {
            return;
        }
        RtpManager.Result result = rtpManager.attempt(player,
                () -> player.sendMessage(color("&aTeleported to a random location.")));
        if (result == RtpManager.Result.BUSY) {
            queue.add(next);
        } else if (result == RtpManager.Result.ON_COOLDOWN) {
            player.sendMessage(color("&cYour random-teleport cooldown wasn't up yet - dropped from the queue."));
        }
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
