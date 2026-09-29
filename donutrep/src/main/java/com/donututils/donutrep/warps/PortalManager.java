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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Physical world portals: a rectangular region that, when a player walks into it, teleports them to
 * a linked warp - a lighter-weight native equivalent to Multiverse's portal blocks (Multiverse-Core
 * is only a softdepend, not something this plugin builds against directly).
 */
public final class PortalManager {

    public record Portal(int id, World world, double minX, double minY, double minZ,
                          double maxX, double maxY, double maxZ, String destinationWarp) {
        public boolean contains(Location location) {
            return location.getWorld().equals(world)
                    && location.getX() >= minX && location.getX() <= maxX
                    && location.getY() >= minY && location.getY() <= maxY
                    && location.getZ() >= minZ && location.getZ() <= maxZ;
        }
    }

    private final Plugin plugin;
    private final DatabaseManager database;
    private final List<Portal> portals = new ArrayList<>();
    private final Map<UUID, Location> pos1 = new ConcurrentHashMap<>();
    private final Map<UUID, Location> pos2 = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTeleportAtMillis = new ConcurrentHashMap<>();

    public PortalManager(Plugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        loadAll();
    }

    private void loadAll() {
        String sql = "SELECT * FROM portals";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                World world = Bukkit.getWorld(rows.getString("world"));
                if (world == null) {
                    continue;
                }
                portals.add(new Portal(rows.getInt("id"), world,
                        rows.getDouble("min_x"), rows.getDouble("min_y"), rows.getDouble("min_z"),
                        rows.getDouble("max_x"), rows.getDouble("max_y"), rows.getDouble("max_z"),
                        rows.getString("destination_warp")));
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load portals", ex);
        }
    }

    public void setPos1(UUID playerId, Location location) {
        pos1.put(playerId, location.clone());
    }

    public void setPos2(UUID playerId, Location location) {
        pos2.put(playerId, location.clone());
    }

    public String create(UUID playerId, String destinationWarp) {
        Location a = pos1.get(playerId);
        Location b = pos2.get(playerId);
        if (a == null || b == null) {
            return "Set both corners first with /portalmanager pos1 and /portalmanager pos2.";
        }
        if (!a.getWorld().equals(b.getWorld())) {
            return "Both corners must be in the same world.";
        }
        String sql = "INSERT INTO portals (world, min_x, min_y, min_z, max_x, max_y, max_z, destination_warp) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, a.getWorld().getName());
            statement.setDouble(2, Math.min(a.getX(), b.getX()));
            statement.setDouble(3, Math.min(a.getY(), b.getY()));
            statement.setDouble(4, Math.min(a.getZ(), b.getZ()));
            statement.setDouble(5, Math.max(a.getX(), b.getX()));
            statement.setDouble(6, Math.max(a.getY(), b.getY()));
            statement.setDouble(7, Math.max(a.getZ(), b.getZ()));
            statement.setString(8, destinationWarp.toLowerCase());
            statement.executeUpdate();
            int id;
            try (ResultSet keys = statement.getGeneratedKeys()) {
                id = keys.next() ? keys.getInt(1) : portals.size() + 1;
            }
            portals.add(new Portal(id, a.getWorld(), Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()),
                    Math.min(a.getZ(), b.getZ()), Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()),
                    Math.max(a.getZ(), b.getZ()), destinationWarp.toLowerCase()));
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create portal", ex);
            return "Failed to save the portal - see console.";
        }
        pos1.remove(playerId);
        pos2.remove(playerId);
        return null;
    }

    public boolean delete(int id) {
        boolean removed = portals.removeIf(p -> p.id() == id);
        if (removed) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                String sql = "DELETE FROM portals WHERE id = ?";
                try (Connection connection = database.getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, id);
                    statement.executeUpdate();
                } catch (SQLException ex) {
                    plugin.getLogger().log(Level.WARNING, "Failed to delete portal #" + id, ex);
                }
            });
        }
        return removed;
    }

    public List<Portal> allPortals() {
        return List.copyOf(portals);
    }

    public Portal find(Location location) {
        for (Portal portal : portals) {
            if (portal.contains(location)) {
                return portal;
            }
        }
        return null;
    }

    /** Basic re-trigger guard so stepping through a portal doesn't immediately bounce back and
     * forth if the destination warp is itself near another portal. */
    public boolean canTeleport(UUID playerId) {
        Long last = lastTeleportAtMillis.get(playerId);
        return last == null || System.currentTimeMillis() - last > 2000;
    }

    public void markTeleported(UUID playerId) {
        lastTeleportAtMillis.put(playerId, System.currentTimeMillis());
    }
}
