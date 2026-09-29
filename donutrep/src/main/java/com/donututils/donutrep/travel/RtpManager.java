package com.donututils.donutrep.travel;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/** Random-teleport search: picks an X/Z within [minRadius, maxRadius) of world spawn, lands on the
 * highest surface block there. Deliberately simple - it doesn't scan for water/lava/void, since doing
 * that without loading/generating chunks synchronously would stall the server; landing on the surface
 * highest-block is a reasonable v1 rather than a full safe-teleport algorithm. */
public final class RtpManager {

    public enum Result { TELEPORTED, ON_COOLDOWN, BUSY, FAILED }

    private final Plugin plugin;
    private final Supplier<RtpConfig> configSupplier;
    private final Random random = new Random();
    private final Map<UUID, Long> lastRtpAtMillis = new ConcurrentHashMap<>();
    private final AtomicInteger activeSearches = new AtomicInteger();

    public RtpManager(Plugin plugin, Supplier<RtpConfig> configSupplier) {
        this.plugin = plugin;
        this.configSupplier = configSupplier;
    }

    /** Attempts an immediate RTP. Returns BUSY if too many searches are already in flight - the
     * caller (RtpCommand) tells the player to use /rtpq instead, which paces requests through a
     * queue rather than piling more concurrent searches on top. */
    public Result attempt(Player player, Runnable onSuccess) {
        RtpConfig config = configSupplier.get();
        long now = System.currentTimeMillis();
        Long last = lastRtpAtMillis.get(player.getUniqueId());
        if (last != null && now - last < config.cooldownSeconds() * 1000L) {
            return Result.ON_COOLDOWN;
        }
        if (activeSearches.get() >= config.maxConcurrentSearches()) {
            return Result.BUSY;
        }
        activeSearches.incrementAndGet();
        lastRtpAtMillis.put(player.getUniqueId(), now);
        World world = player.getWorld();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Location target = findSafeLocation(world, config);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                activeSearches.decrementAndGet();
                if (target != null && player.isOnline()) {
                    player.teleport(target);
                    onSuccess.run();
                }
            });
        });
        return Result.TELEPORTED;
    }

    private Location findSafeLocation(World world, RtpConfig config) {
        for (int i = 0; i < config.maxAttempts(); i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = config.minRadius() + random.nextDouble() * (config.maxRadius() - config.minRadius());
            int x = (int) (Math.cos(angle) * distance);
            int z = (int) (Math.sin(angle) * distance);
            int y = world.getHighestBlockYAt(x, z);
            if (y > 0) {
                return new Location(world, x + 0.5, y + 1, z + 0.5);
            }
        }
        return null;
    }
}
