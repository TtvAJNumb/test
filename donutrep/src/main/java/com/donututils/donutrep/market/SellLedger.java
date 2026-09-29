package com.donututils.donutrep.market;

import com.donututils.donutrep.market.db.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/** Records every sell transaction (from /sellhand, /sellall, /sellmulti, or the /shop GUI's
 * shift-click sell) so /sellhistory, /sellprogress, and /topsell have something to read. */
public final class SellLedger {

    public record Entry(Material material, int amount, double totalPrice, long soldAtMillis) {
    }

    public record TopSeller(UUID playerId, String playerName, double totalEarned) {
    }

    private final Plugin plugin;
    private final DatabaseManager database;

    public SellLedger(Plugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
    }

    public void record(Player player, Material material, int amount, double totalPrice) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO sell_log (player_id, player_name, material, amount, total_price, sold_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, player.getUniqueId().toString());
                statement.setString(2, player.getName());
                statement.setString(3, material.name());
                statement.setInt(4, amount);
                statement.setDouble(5, totalPrice);
                statement.setLong(6, System.currentTimeMillis());
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to record sell for " + player.getName(), ex);
            }
        });
    }

    public List<Entry> recentSales(UUID playerId, int limit) {
        String sql = "SELECT material, amount, total_price, sold_at FROM sell_log WHERE player_id = ? "
                + "ORDER BY sold_at DESC LIMIT ?";
        List<Entry> results = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setInt(2, limit);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    results.add(new Entry(Material.valueOf(rows.getString("material")), rows.getInt("amount"),
                            rows.getDouble("total_price"), rows.getLong("sold_at")));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to read sell history for " + playerId, ex);
        }
        return results;
    }

    /** Total Money earned and total items sold, lifetime, for this player. */
    public double lifetimeEarnings(UUID playerId) {
        String sql = "SELECT COALESCE(SUM(total_price), 0) AS total FROM sell_log WHERE player_id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getDouble("total") : 0.0;
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to read lifetime earnings for " + playerId, ex);
            return 0.0;
        }
    }

    public long lifetimeItemsSold(UUID playerId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) AS total FROM sell_log WHERE player_id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getLong("total") : 0L;
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to read lifetime items sold for " + playerId, ex);
            return 0L;
        }
    }

    public List<TopSeller> topSellers(int limit) {
        String sql = "SELECT player_id, player_name, SUM(total_price) AS total FROM sell_log "
                + "GROUP BY player_id ORDER BY total DESC LIMIT ?";
        List<TopSeller> results = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    results.add(new TopSeller(UUID.fromString(rows.getString("player_id")),
                            rows.getString("player_name"), rows.getDouble("total")));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to read top sellers", ex);
        }
        return results;
    }
}
