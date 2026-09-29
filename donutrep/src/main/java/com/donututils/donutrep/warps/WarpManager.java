package com.donututils.donutrep.warps;

import com.donututils.donutrep.warps.db.DatabaseManager;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** Admin-set named warps (see /warp, /setwarp, /delwarp, /warpmanager) - loaded once at startup into
 * an in-memory cache, write-through on every change. */
public final class WarpManager {

    private final Plugin plugin;
    private final DatabaseManager database;
    private final Map<String, Location> warps = new ConcurrentHashMap<>();

    public WarpManager(Plugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        loadAll();
    }

    private void loadAll() {
        String sql = "SELECT name, world, x, y, z, yaw, pitch FROM warps";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                World world = Bukkit.getWorld(rows.getString("world"));
                if (world == null) {
                    continue;
                }
                Location location = new Location(world, rows.getDouble("x"), rows.getDouble("y"), rows.getDouble("z"),
                        (float) rows.getDouble("yaw"), (float) rows.getDouble("pitch"));
                warps.put(rows.getString("name").toLowerCase(), location);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load warps", ex);
        }
    }

    public Location warp(String name) {
        return warps.get(name.toLowerCase());
    }

    public List<String> warpNames() {
        return List.copyOf(new TreeMap<>(warps).keySet());
    }

    public void setWarp(String name, Location location) {
        warps.put(name.toLowerCase(), location.clone());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO warps (name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?) "
                    + "ON CONFLICT(name) DO UPDATE SET world = excluded.world, x = excluded.x, y = excluded.y, "
                    + "z = excluded.z, yaw = excluded.yaw, pitch = excluded.pitch";
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, name.toLowerCase());
                statement.setString(2, location.getWorld().getName());
                statement.setDouble(3, location.getX());
                statement.setDouble(4, location.getY());
                statement.setDouble(5, location.getZ());
                statement.setDouble(6, location.getYaw());
                statement.setDouble(7, location.getPitch());
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to persist warp '" + name + "'", ex);
            }
        });
    }

    public boolean deleteWarp(String name) {
        boolean removed = warps.remove(name.toLowerCase()) != null;
        if (removed) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                String sql = "DELETE FROM warps WHERE name = ?";
                try (Connection connection = database.getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, name.toLowerCase());
                    statement.executeUpdate();
                } catch (SQLException ex) {
                    plugin.getLogger().log(Level.WARNING, "Failed to delete warp '" + name + "'", ex);
                }
            });
        }
        return removed;
    }
}
