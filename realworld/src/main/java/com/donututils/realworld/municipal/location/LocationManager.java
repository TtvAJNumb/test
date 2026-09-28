package com.donututils.realworld.municipal.location;

import com.donututils.realworld.municipal.db.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Staff-defined named places - a point warp (like "market"), or a full cuboid region with a
 * teleport point (like "jail" or a courthouse trial area). Selection of the two corners works like
 * a WorldEdit-style wand, except it's just two commands ({@code /location pos1}/{@code pos2}) acting
 * on wherever the player is currently standing - no special item needed. Selections are per-player
 * and in-memory only (a live staff workflow, not something that needs to survive a restart).
 */
public final class LocationManager {

    private final Plugin plugin;
    private final DatabaseManager database;

    private final Map<String, NamedLocation> locations = new ConcurrentHashMap<>();
    private final Map<UUID, Location> pos1Selections = new ConcurrentHashMap<>();
    private final Map<UUID, Location> pos2Selections = new ConcurrentHashMap<>();

    public LocationManager(Plugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        loadAll();
    }

    private void loadAll() {
        String sql = "SELECT name, world, x, y, z, yaw, pitch, has_bounds, min_x, min_y, min_z, max_x, max_y, max_z FROM locations";
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                String name = rows.getString("name");
                Cuboid bounds = null;
                if (rows.getInt("has_bounds") != 0) {
                    bounds = new Cuboid(rows.getString("world"),
                            rows.getDouble("min_x"), rows.getDouble("min_y"), rows.getDouble("min_z"),
                            rows.getDouble("max_x"), rows.getDouble("max_y"), rows.getDouble("max_z"));
                }
                locations.put(name.toLowerCase(Locale.ROOT), new NamedLocation(name, rows.getString("world"),
                        rows.getDouble("x"), rows.getDouble("y"), rows.getDouble("z"),
                        rows.getFloat("yaw"), rows.getFloat("pitch"), bounds));
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load locations", ex);
        }
        plugin.getLogger().info("Loaded " + locations.size() + " named location(s).");
    }

    public void setPos1(Player player) {
        pos1Selections.put(player.getUniqueId(), player.getLocation());
    }

    public void setPos2(Player player) {
        pos2Selections.put(player.getUniqueId(), player.getLocation());
    }

    public void clearSelection(Player player) {
        pos1Selections.remove(player.getUniqueId());
        pos2Selections.remove(player.getUniqueId());
    }

    public record SaveResult(boolean success, String message) {
    }

    /** Saves the player's current position as the teleport point. If they have both corners of a
     * selection set (and it's on the same world), the location also gets cuboid bounds. */
    public SaveResult save(String rawName, Player player) {
        String key = rawName.toLowerCase(Locale.ROOT);
        Location current = player.getLocation();
        Location pos1 = pos1Selections.get(player.getUniqueId());
        Location pos2 = pos2Selections.get(player.getUniqueId());

        Cuboid bounds = null;
        if (pos1 != null && pos2 != null) {
            if (pos1.getWorld() == null || pos2.getWorld() == null || !pos1.getWorld().equals(pos2.getWorld())) {
                return new SaveResult(false, "Your pos1 and pos2 are in different worlds - clear your selection and try again.");
            }
            bounds = Cuboid.of(pos1, pos2);
        }

        NamedLocation location = new NamedLocation(key, current.getWorld().getName(),
                current.getX(), current.getY(), current.getZ(), current.getYaw(), current.getPitch(), bounds);
        locations.put(key, location);
        persistAsync(location);
        clearSelection(player);

        return new SaveResult(true, bounds != null
                ? "Saved '" + key + "' as a cuboid region (teleport point = where you're standing)."
                : "Saved '" + key + "' as a point warp.");
    }

    public boolean remove(String name) {
        boolean existed = locations.remove(name.toLowerCase(Locale.ROOT)) != null;
        if (existed) {
            deleteAsync(name);
        }
        return existed;
    }

    public NamedLocation get(String name) {
        return locations.get(name.toLowerCase(Locale.ROOT));
    }

    public java.util.Collection<NamedLocation> all() {
        return locations.values();
    }

    private void persistAsync(NamedLocation location) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = """
                    INSERT INTO locations (name, world, x, y, z, yaw, pitch, has_bounds, min_x, min_y, min_z, max_x, max_y, max_z)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(name) DO UPDATE SET
                        world = excluded.world, x = excluded.x, y = excluded.y, z = excluded.z,
                        yaw = excluded.yaw, pitch = excluded.pitch, has_bounds = excluded.has_bounds,
                        min_x = excluded.min_x, min_y = excluded.min_y, min_z = excluded.min_z,
                        max_x = excluded.max_x, max_y = excluded.max_y, max_z = excluded.max_z
                    """;
            try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                Cuboid bounds = location.bounds();
                statement.setString(1, location.name());
                statement.setString(2, location.world());
                Location point = location.toLocation();
                statement.setDouble(3, point != null ? point.getX() : 0);
                statement.setDouble(4, point != null ? point.getY() : 0);
                statement.setDouble(5, point != null ? point.getZ() : 0);
                statement.setFloat(6, point != null ? point.getYaw() : 0f);
                statement.setFloat(7, point != null ? point.getPitch() : 0f);
                statement.setInt(8, bounds != null ? 1 : 0);
                statement.setObject(9, bounds != null ? bounds.minX() : null);
                statement.setObject(10, bounds != null ? bounds.minY() : null);
                statement.setObject(11, bounds != null ? bounds.minZ() : null);
                statement.setObject(12, bounds != null ? bounds.maxX() : null);
                statement.setObject(13, bounds != null ? bounds.maxY() : null);
                statement.setObject(14, bounds != null ? bounds.maxZ() : null);
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to persist location " + location.name(), ex);
            }
        });
    }

    private void deleteAsync(String name) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM locations WHERE name = ?")) {
                statement.setString(1, name.toLowerCase(Locale.ROOT));
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to delete location " + name, ex);
            }
        });
    }
}
