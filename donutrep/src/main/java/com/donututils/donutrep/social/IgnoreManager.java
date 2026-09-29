package com.donututils.donutrep.social;

import com.donututils.donutrep.social.db.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** Who's ignoring who - a player that ignores someone stops receiving their private messages and
 * their global/local chat lines. Loaded once at startup into an in-memory cache, write-behind
 * persisted, same pattern as the economy balances cache. */
public final class IgnoreManager {

    private final Plugin plugin;
    private final DatabaseManager database;
    private final Map<UUID, Set<UUID>> ignoring = new ConcurrentHashMap<>();

    public IgnoreManager(Plugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        loadAll();
    }

    private void loadAll() {
        String sql = "SELECT player_id, ignored_id FROM ignores";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                try {
                    UUID player = UUID.fromString(rows.getString("player_id"));
                    UUID ignored = UUID.fromString(rows.getString("ignored_id"));
                    ignoring.computeIfAbsent(player, id -> ConcurrentHashMap.newKeySet()).add(ignored);
                } catch (IllegalArgumentException ignoredEx) {
                    // skip malformed row
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load ignore list", ex);
        }
    }

    public boolean isIgnoring(UUID player, UUID target) {
        Set<UUID> set = ignoring.get(player);
        return set != null && set.contains(target);
    }

    public boolean ignore(UUID player, UUID target) {
        boolean added = ignoring.computeIfAbsent(player, id -> ConcurrentHashMap.newKeySet()).add(target);
        if (added) {
            persistAsync(player, target, true);
        }
        return added;
    }

    public boolean unignore(UUID player, UUID target) {
        Set<UUID> set = ignoring.get(player);
        boolean removed = set != null && set.remove(target);
        if (removed) {
            persistAsync(player, target, false);
        }
        return removed;
    }

    private void persistAsync(UUID player, UUID target, boolean adding) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = adding
                    ? "INSERT OR IGNORE INTO ignores (player_id, ignored_id) VALUES (?, ?)"
                    : "DELETE FROM ignores WHERE player_id = ? AND ignored_id = ?";
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, player.toString());
                statement.setString(2, target.toString());
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to persist ignore list change", ex);
            }
        });
    }
}
