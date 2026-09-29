package com.donututils.donutrep.orders;

import com.donututils.donutrep.economy.EconomyManager;
import com.donututils.donutrep.orders.db.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * The Orders board: post a "want to buy" request for an item at a price, escrowing the total cost
 * up front; any other player holding a matching stack can fulfill it and gets paid immediately.
 * The bought item sits against the order until the buyer runs /orders collect - there's no delivery
 * while offline, matching the Auction House's same "collect it yourself" simplification.
 */
public final class OrderBoardManager {

    public enum CreateResult { SUCCESS, INVALID_PRICE, INVALID_AMOUNT, INSUFFICIENT_FUNDS }
    public enum FulfillResult { SUCCESS, NOT_FOUND, ALREADY_FULFILLED, OWN_ORDER, NOT_ENOUGH_ITEMS }

    private final Plugin plugin;
    private final DatabaseManager database;
    private final EconomyManager economy;

    public OrderBoardManager(Plugin plugin, DatabaseManager database, EconomyManager economy) {
        this.plugin = plugin;
        this.database = database;
        this.economy = economy;
    }

    public CreateResult create(Player buyer, Material material, int amount, double pricePerUnit) {
        if (pricePerUnit <= 0) {
            return CreateResult.INVALID_PRICE;
        }
        if (amount <= 0) {
            return CreateResult.INVALID_AMOUNT;
        }
        double total = pricePerUnit * amount;
        if (!economy.has(buyer, total)) {
            return CreateResult.INSUFFICIENT_FUNDS;
        }
        economy.withdrawPlayer(buyer, total);
        String sql = "INSERT INTO buy_orders (buyer_id, buyer_name, material, amount, price_per_unit, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, buyer.getUniqueId().toString());
            statement.setString(2, buyer.getName());
            statement.setString(3, material.name());
            statement.setInt(4, amount);
            statement.setDouble(5, pricePerUnit);
            statement.setLong(6, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create order", ex);
        }
        return CreateResult.SUCCESS;
    }

    public FulfillResult fulfill(Player fulfiller, int orderId) {
        BuyOrder order = find(orderId);
        if (order == null) {
            return FulfillResult.NOT_FOUND;
        }
        if (order.isFulfilled()) {
            return FulfillResult.ALREADY_FULFILLED;
        }
        if (order.buyerId().equals(fulfiller.getUniqueId())) {
            return FulfillResult.OWN_ORDER;
        }
        if (!removeMaterial(fulfiller, order.material(), order.amount())) {
            return FulfillResult.NOT_ENOUGH_ITEMS;
        }
        String sql = "UPDATE buy_orders SET fulfiller_id = ?, fulfiller_name = ?, fulfilled_at = ? WHERE id = ? AND fulfiller_id IS NULL";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fulfiller.getUniqueId().toString());
            statement.setString(2, fulfiller.getName());
            statement.setLong(3, System.currentTimeMillis());
            statement.setInt(4, orderId);
            if (statement.executeUpdate() == 0) {
                // someone else fulfilled it first - give the items back
                fulfiller.getInventory().addItem(new ItemStack(order.material(), order.amount()));
                return FulfillResult.ALREADY_FULFILLED;
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to fulfill order #" + orderId, ex);
        }
        economy.depositPlayer(fulfiller, order.totalPrice());
        return FulfillResult.SUCCESS;
    }

    /** Delivers every item this player is owed from fulfilled-but-uncollected orders, returning how
     * many were delivered. */
    public int collect(Player buyer) {
        List<BuyOrder> toCollect = new ArrayList<>();
        String sql = "SELECT * FROM buy_orders WHERE buyer_id = ? AND fulfiller_id IS NOT NULL AND collected = 0";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, buyer.getUniqueId().toString());
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    toCollect.add(map(rows));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to look up collectible orders for " + buyer.getUniqueId(), ex);
            return 0;
        }
        for (BuyOrder order : toCollect) {
            buyer.getInventory().addItem(new ItemStack(order.material(), order.amount()));
            markCollected(order.id());
        }
        return toCollect.size();
    }

    public boolean cancel(Player buyer, int orderId) {
        BuyOrder order = find(orderId);
        if (order == null || !order.buyerId().equals(buyer.getUniqueId()) || order.isFulfilled()) {
            return false;
        }
        if (!delete(orderId)) {
            return false;
        }
        economy.depositPlayer(buyer, order.totalPrice());
        return true;
    }

    public BuyOrder find(int orderId) {
        String sql = "SELECT * FROM buy_orders WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? map(rows) : null;
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to look up order #" + orderId, ex);
            return null;
        }
    }

    public List<BuyOrder> activeOrders() {
        return query("SELECT * FROM buy_orders WHERE fulfiller_id IS NULL ORDER BY created_at DESC");
    }

    public List<BuyOrder> ordersByBuyer(UUID buyerId) {
        String sql = "SELECT * FROM buy_orders WHERE buyer_id = ? ORDER BY created_at DESC";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, buyerId.toString());
            List<BuyOrder> results = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    results.add(map(rows));
                }
            }
            return results;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to look up orders for " + buyerId, ex);
            return List.of();
        }
    }

    private void markCollected(int orderId) {
        String sql = "UPDATE buy_orders SET collected = 1 WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to mark order #" + orderId + " collected", ex);
        }
    }

    private boolean delete(int orderId) {
        String sql = "DELETE FROM buy_orders WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete order #" + orderId, ex);
            return false;
        }
    }

    private List<BuyOrder> query(String sql) {
        List<BuyOrder> results = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                results.add(map(rows));
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to query orders", ex);
        }
        return results;
    }

    private BuyOrder map(ResultSet rows) throws SQLException {
        String fulfillerId = rows.getString("fulfiller_id");
        return new BuyOrder(
                rows.getInt("id"),
                UUID.fromString(rows.getString("buyer_id")),
                rows.getString("buyer_name"),
                Material.valueOf(rows.getString("material")),
                rows.getInt("amount"),
                rows.getDouble("price_per_unit"),
                fulfillerId != null ? UUID.fromString(fulfillerId) : null,
                rows.getString("fulfiller_name"),
                rows.getInt("collected") != 0
        );
    }

    private boolean removeMaterial(Player player, Material material, int amount) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) {
                total += stack.getAmount();
            }
        }
        if (total < amount) {
            return false;
        }
        ItemStack[] contents = player.getInventory().getContents();
        int remaining = amount;
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != material) {
                continue;
            }
            int take = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            remaining -= take;
            player.getInventory().setItem(i, stack.getAmount() <= 0 ? null : stack);
        }
        return true;
    }
}
