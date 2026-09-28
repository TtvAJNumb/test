package com.donututils.municipal.jail;

import com.donututils.municipal.config.MunicipalConfig;
import com.donututils.municipal.db.DatabaseManager;
import com.donututils.municipal.location.LocationManager;
import com.donututils.municipal.location.NamedLocation;
import com.donututils.municipal.model.JailRecord;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Actually restricts a jailed player: teleports them to (and keeps them within a radius of) a
 * configured jail point, and - via {@link JailListener} - blocks every command except a small
 * whitelist, until their sentence elapses.
 */
public final class JailManager {

    private final Plugin plugin;
    private final DatabaseManager database;
    private final Supplier<MunicipalConfig> configSupplier;
    private final LocationManager locationManager;

    private final Map<UUID, JailRecord> jailed = new ConcurrentHashMap<>();

    public JailManager(Plugin plugin, DatabaseManager database, Supplier<MunicipalConfig> configSupplier,
                        LocationManager locationManager) {
        this.plugin = plugin;
        this.database = database;
        this.configSupplier = configSupplier;
        this.locationManager = locationManager;
        loadAll();
    }

    private void loadAll() {
        String sql = "SELECT player_id, case_id, jailed_until, origin_world, origin_x, origin_y, origin_z FROM jail_records";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                try {
                    UUID playerId = UUID.fromString(rows.getString("player_id"));
                    jailed.put(playerId, new JailRecord(playerId, rows.getLong("case_id"), rows.getLong("jailed_until"),
                            rows.getString("origin_world"), rows.getDouble("origin_x"), rows.getDouble("origin_y"), rows.getDouble("origin_z")));
                } catch (IllegalArgumentException ignored) {
                    // skip malformed row
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load jail records", ex);
        }
    }

    public boolean isJailed(UUID playerId) {
        JailRecord record = jailed.get(playerId);
        return record != null && record.jailedUntilMillis() > System.currentTimeMillis();
    }

    public JailRecord getRecord(UUID playerId) {
        return jailed.get(playerId);
    }

    /** Must be called on the main thread - it teleports. */
    public void jailPlayer(Player player, long caseId, int minutes) {
        Location origin = player.getLocation();
        JailRecord record = new JailRecord(player.getUniqueId(), caseId,
                System.currentTimeMillis() + minutes * 60_000L,
                origin.getWorld() != null ? origin.getWorld().getName() : "world",
                origin.getX(), origin.getY(), origin.getZ());
        jailed.put(player.getUniqueId(), record);
        teleportToJail(player);
        persistAsync(record);
    }

    /** Must be called on the main thread - it teleports. */
    public void releasePlayer(UUID playerId) {
        JailRecord record = jailed.remove(playerId);
        Player player = Bukkit.getServer().getPlayer(playerId);
        if (player != null && record != null) {
            World world = Bukkit.getWorld(record.originWorld());
            if (world != null) {
                player.teleport(new Location(world, record.originX(), record.originY(), record.originZ()));
            }
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&aYour sentence is over - you've been released."));
        }
        deleteAsync(playerId);
    }

    /** Call on join: if this player logged off mid-sentence, put them straight back in the jail
     * point rather than wherever the client tries to spawn them. */
    public void onRejoin(Player player) {
        if (isJailed(player.getUniqueId())) {
            teleportToJail(player);
        }
    }

    private void teleportToJail(Player player) {
        NamedLocation named = locationManager.get("jail");
        if (named != null) {
            Location target = named.toLocation();
            if (target != null) {
                player.teleport(target);
                return;
            }
        }

        MunicipalConfig config = configSupplier.get();
        World world = Bukkit.getWorld(config.jailWorld());
        if (world == null) {
            plugin.getLogger().warning("Jail world '" + config.jailWorld() + "' doesn't exist - couldn't teleport " + player.getName());
            return;
        }
        player.teleport(new Location(world, config.jailX(), config.jailY(), config.jailZ()));
    }

    /** Called from a repeating main-thread task: snaps anyone who's wandered outside the jail area
     * back to its teleport point. If a "jail" location has been defined with {@code /location
     * pos1}/{@code pos2}/{@code save jail}, its exact cuboid bounds are enforced; otherwise this
     * falls back to the old point+radius config. */
    public void enforceRadius(Player player) {
        NamedLocation named = locationManager.get("jail");
        if (named != null) {
            Location target = named.toLocation();
            if (target == null) {
                return;
            }
            if (named.hasBounds()) {
                if (!named.bounds().contains(player.getLocation())) {
                    player.teleport(target);
                }
            } else if (player.getLocation().getWorld() != target.getWorld() || player.getLocation().distance(target) > configSupplier.get().jailRadius()) {
                player.teleport(target);
            }
            return;
        }

        MunicipalConfig config = configSupplier.get();
        World jailWorld = Bukkit.getWorld(config.jailWorld());
        if (jailWorld == null) {
            return;
        }
        Location jailPoint = new Location(jailWorld, config.jailX(), config.jailY(), config.jailZ());
        Location current = player.getLocation();
        if (current.getWorld() != jailPoint.getWorld() || current.distance(jailPoint) > config.jailRadius()) {
            player.teleport(jailPoint);
        }
    }

    /** Called from a repeating main-thread task: releases anyone whose sentence has elapsed. */
    public void tickReleases() {
        long now = System.currentTimeMillis();
        for (UUID playerId : Map.copyOf(jailed).keySet()) {
            JailRecord record = jailed.get(playerId);
            if (record != null && record.jailedUntilMillis() <= now) {
                releasePlayer(playerId);
            }
        }
    }

    private void persistAsync(JailRecord record) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO jail_records (player_id, case_id, jailed_until, origin_world, origin_x, origin_y, origin_z) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                    + "ON CONFLICT(player_id) DO UPDATE SET case_id = excluded.case_id, jailed_until = excluded.jailed_until, "
                    + "origin_world = excluded.origin_world, origin_x = excluded.origin_x, origin_y = excluded.origin_y, origin_z = excluded.origin_z";
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, record.playerId().toString());
                statement.setLong(2, record.caseId());
                statement.setLong(3, record.jailedUntilMillis());
                statement.setString(4, record.originWorld());
                statement.setDouble(5, record.originX());
                statement.setDouble(6, record.originY());
                statement.setDouble(7, record.originZ());
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to persist jail record for " + record.playerId(), ex);
            }
        });
    }

    private void deleteAsync(UUID playerId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM jail_records WHERE player_id = ?")) {
                statement.setString(1, playerId.toString());
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to delete jail record for " + playerId, ex);
            }
        });
    }
}
