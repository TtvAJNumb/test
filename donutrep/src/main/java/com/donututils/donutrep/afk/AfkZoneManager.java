package com.donututils.donutrep.afk;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional AFK lounge: when enabled and a zone location is configured, going AFK teleports the
 * player there (out of the way of wherever they were standing) and returning from AFK teleports
 * them back. Disabled by default (afk.zone.enabled: false in config.yml) - when disabled, AFK
 * players just stay wherever they logged off/idled, same as DonutREP's original behavior.
 */
public final class AfkZoneManager implements AfkManager.StatusListener {

    private final JavaPlugin plugin;
    private volatile boolean enabled;
    private volatile Location zoneLocation;
    private final Map<UUID, Location> preAfkLocation = new ConcurrentHashMap<>();

    public AfkZoneManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("afk.zone.enabled", false);
        String worldName = plugin.getConfig().getString("afk.zone.world", "");
        if (!worldName.isBlank()) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                this.zoneLocation = new Location(world,
                        plugin.getConfig().getDouble("afk.zone.x", 0.0),
                        plugin.getConfig().getDouble("afk.zone.y", 64.0),
                        plugin.getConfig().getDouble("afk.zone.z", 0.0));
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Sets (and enables) the AFK zone to this location, persisting it to config.yml. */
    public void setZone(Location location) {
        this.zoneLocation = location.clone();
        this.enabled = true;
        plugin.getConfig().set("afk.zone.enabled", true);
        plugin.getConfig().set("afk.zone.world", location.getWorld().getName());
        plugin.getConfig().set("afk.zone.x", location.getX());
        plugin.getConfig().set("afk.zone.y", location.getY());
        plugin.getConfig().set("afk.zone.z", location.getZ());
        plugin.saveConfig();
    }

    @Override
    public void onAfkChanged(Player player, boolean afk) {
        if (!enabled || zoneLocation == null) {
            return;
        }
        if (afk) {
            preAfkLocation.put(player.getUniqueId(), player.getLocation());
            player.teleport(zoneLocation);
        } else {
            Location previous = preAfkLocation.remove(player.getUniqueId());
            if (previous != null) {
                player.teleport(previous);
            }
        }
    }
}
