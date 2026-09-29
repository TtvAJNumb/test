package com.donututils.donutrep.homes;

import com.donututils.donutrep.homes.db.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;
import java.util.logging.Level;

/** Per-player named homes - loaded once at startup into an in-memory cache (write-through on every
 * change), same pattern as every other subsystem's cache-backed manager in this plugin. */
public final class HomeManager {

    public enum Result { CREATED, UPDATED, LIMIT_REACHED, NOT_FOUND, ALREADY_EXISTS }

    private final Plugin plugin;
    private final DatabaseManager database;
    private final IntSupplier maxHomesSupplier;
    private final Map<UUID, Map<String, Location>> homes = new ConcurrentHashMap<>();

    public HomeManager(Plugin plugin, DatabaseManager database, IntSupplier maxHomesSupplier) {
        this.plugin = plugin;
        this.database = database;
        this.maxHomesSupplier = maxHomesSupplier;
        loadAll();
    }

    private void loadAll() {
        String sql = "SELECT player_id, name, world, x, y, z, yaw, pitch FROM homes";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                try {
                    UUID playerId = UUID.fromString(rows.getString("player_id"));
                    World world = Bukkit.getWorld(rows.getString("world"));
                    if (world == null) {
                        continue;
                    }
                    Location location = new Location(world, rows.getDouble("x"), rows.getDouble("y"), rows.getDouble("z"),
                            (float) rows.getDouble("yaw"), (float) rows.getDouble("pitch"));
                    homes.computeIfAbsent(playerId, id -> new ConcurrentHashMap<>()).put(rows.getString("name").toLowerCase(), location);
                } catch (IllegalArgumentException ignored) {
                    // skip malformed row
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load homes", ex);
        }
    }

    public Location home(UUID playerId, String name) {
        Map<String, Location> playerHomes = homes.get(playerId);
        return playerHomes == null ? null : playerHomes.get(name.toLowerCase());
    }

    public List<String> homeNames(UUID playerId) {
        Map<String, Location> playerHomes = homes.get(playerId);
        return playerHomes == null ? List.of() : new TreeMap<>(playerHomes).keySet().stream().toList();
    }

    public Result setHome(UUID playerId, String name, Location location) {
        Map<String, Location> playerHomes = homes.computeIfAbsent(playerId, id -> new ConcurrentHashMap<>());
        String key = name.toLowerCase();
        boolean existed = playerHomes.containsKey(key);
        if (!existed && playerHomes.size() >= maxHomesSupplier.getAsInt()) {
            return Result.LIMIT_REACHED;
        }
        playerHomes.put(key, location.clone());
        persistAsync(playerId, name, location);
        return existed ? Result.UPDATED : Result.CREATED;
    }

    public Result deleteHome(UUID playerId, String name) {
        Map<String, Location> playerHomes = homes.get(playerId);
        String key = name.toLowerCase();
        if (playerHomes == null || playerHomes.remove(key) == null) {
            return Result.NOT_FOUND;
        }
        deleteAsync(playerId, name);
        return Result.UPDATED;
    }

    public Result renameHome(UUID playerId, String oldName, String newName) {
        Map<String, Location> playerHomes = homes.get(playerId);
        String oldKey = oldName.toLowerCase();
        String newKey = newName.toLowerCase();
        if (playerHomes == null || !playerHomes.containsKey(oldKey)) {
            return Result.NOT_FOUND;
        }
        if (playerHomes.containsKey(newKey)) {
            return Result.ALREADY_EXISTS;
        }
        Location location = playerHomes.remove(oldKey);
        playerHomes.put(newKey, location);
        deleteAsync(playerId, oldName);
        persistAsync(playerId, newName, location);
        return Result.UPDATED;
    }

    private void persistAsync(UUID playerId, String name, Location location) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO homes (player_id, name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                    + "ON CONFLICT(player_id, name) DO UPDATE SET world = excluded.world, x = excluded.x, y = excluded.y, "
                    + "z = excluded.z, yaw = excluded.yaw, pitch = excluded.pitch";
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerId.toString());
                statement.setString(2, name.toLowerCase());
                statement.setString(3, location.getWorld().getName());
                statement.setDouble(4, location.getX());
                statement.setDouble(5, location.getY());
                statement.setDouble(6, location.getZ());
                statement.setDouble(7, location.getYaw());
                statement.setDouble(8, location.getPitch());
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to persist home '" + name + "' for " + playerId, ex);
            }
        });
    }

    private void deleteAsync(UUID playerId, String name) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "DELETE FROM homes WHERE player_id = ? AND name = ?";
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerId.toString());
                statement.setString(2, name.toLowerCase());
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to delete home '" + name + "' for " + playerId, ex);
            }
        });
    }
}
