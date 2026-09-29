package com.donututils.donutrep.sell;

import com.donututils.donutrep.sell.db.DatabaseManager;
import org.bukkit.Bukkit;
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

/** Persists every /sell*-family transaction for /sellprogress, /sellhistory, and /topsell. */
public final class SellLedger {

    public record SaleRecord(String material, int amount, double price, long soldAtMillis) {
    }

    public record TotalsRecord(int itemsSold, double moneyEarned) {
    }

    public record TopSeller(UUID playerId, double moneyEarned) {
    }

    private final Plugin plugin;
    private final DatabaseManager database;

    public SellLedger(Plugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
    }

    public void record(Player player, String materialName, int amount, double price) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = database.getConnection()) {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO sales (player_id, material, amount, price, sold_at) VALUES (?, ?, ?, ?, ?)")) {
                    insert.setString(1, player.getUniqueId().toString());
                    insert.setString(2, materialName);
                    insert.setInt(3, amount);
                    insert.setDouble(4, price);
                    insert.setLong(5, System.currentTimeMillis());
                    insert.executeUpdate();
                }
                try (PreparedStatement upsert = connection.prepareStatement(
                        "INSERT INTO sell_totals (player_id, items_sold, money_earned) VALUES (?, ?, ?) "
                                + "ON CONFLICT(player_id) DO UPDATE SET items_sold = items_sold + excluded.items_sold, "
                                + "money_earned = money_earned + excluded.money_earned")) {
                    upsert.setString(1, player.getUniqueId().toString());
                    upsert.setInt(2, amount);
                    upsert.setDouble(3, price);
                    upsert.executeUpdate();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to record sell transaction", ex);
            }
        });
    }

    public TotalsRecord totalsFor(UUID playerId) {
        String sql = "SELECT items_sold, money_earned FROM sell_totals WHERE player_id = ?";
        try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) {
                    return new TotalsRecord(rows.getInt("items_sold"), rows.getDouble("money_earned"));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to load sell totals", ex);
        }
        return new TotalsRecord(0, 0);
    }

    public List<SaleRecord> recentHistory(UUID playerId, int limit) {
        List<SaleRecord> results = new ArrayList<>();
        String sql = "SELECT material, amount, price, sold_at FROM sales WHERE player_id = ? ORDER BY sold_at DESC LIMIT ?";
        try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setInt(2, limit);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    results.add(new SaleRecord(rows.getString("material"), rows.getInt("amount"),
                            rows.getDouble("price"), rows.getLong("sold_at")));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to load sell history", ex);
        }
        return results;
    }

    public List<TopSeller> topSellers(int limit) {
        List<TopSeller> results = new ArrayList<>();
        String sql = "SELECT player_id, money_earned FROM sell_totals ORDER BY money_earned DESC LIMIT ?";
        try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    results.add(new TopSeller(UUID.fromString(rows.getString("player_id")), rows.getDouble("money_earned")));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to load top sellers", ex);
        }
        return results;
    }
}
