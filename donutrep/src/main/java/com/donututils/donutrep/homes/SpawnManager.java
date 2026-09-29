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
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/** The single server-wide spawn point players return to with /spawn. Falls back to each world's own
 * vanilla spawn point until an admin runs /setspawn at least once. */
public final class SpawnManager {

    private final Plugin plugin;
    private final DatabaseManager database;
    private final AtomicReference<Location> spawn = new AtomicReference<>();

    public SpawnManager(Plugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        load();
    }

    private void load() {
        String sql = "SELECT world, x, y, z, yaw, pitch FROM spawn WHERE id = 1";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            if (rows.next()) {
                World world = Bukkit.getWorld(rows.getString("world"));
                if (world != null) {
                    spawn.set(new Location(world, rows.getDouble("x"), rows.getDouble("y"), rows.getDouble("z"),
                            (float) rows.getDouble("yaw"), (float) rows.getDouble("pitch")));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load spawn", ex);
        }
    }

    /** Returns the admin-set spawn if one exists, otherwise the given world's own vanilla spawn. */
    public Location spawnOrWorldDefault(World fallbackWorld) {
        Location set = spawn.get();
        return set != null ? set.clone() : fallbackWorld.getSpawnLocation();
    }

    public void setSpawn(Location location) {
        spawn.set(location.clone());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO spawn (id, world, x, y, z, yaw, pitch) VALUES (1, ?, ?, ?, ?, ?, ?) "
                    + "ON CONFLICT(id) DO UPDATE SET world = excluded.world, x = excluded.x, y = excluded.y, "
                    + "z = excluded.z, yaw = excluded.yaw, pitch = excluded.pitch";
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, location.getWorld().getName());
                statement.setDouble(2, location.getX());
                statement.setDouble(3, location.getY());
                statement.setDouble(4, location.getZ());
                statement.setDouble(5, location.getYaw());
                statement.setDouble(6, location.getPitch());
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to persist spawn", ex);
            }
        });
    }
}
