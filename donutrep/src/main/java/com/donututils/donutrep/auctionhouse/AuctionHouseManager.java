package com.donututils.donutrep.auctionhouse;

import com.donututils.donutrep.auctionhouse.db.DatabaseManager;
import com.donututils.donutrep.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * A real player-to-player marketplace: list an item from your inventory for a Money price, anyone
 * else can buy it, proceeds go straight to the seller (even while offline). A deliberate scope
 * limit: only the material, stack size, and display name are preserved on a listing - full item NBT
 * (enchants, custom lore, etc) isn't round-tripped, so this isn't a substitute for a plugin like
 * bStats-grade auction houses that snapshot the whole ItemStack, but it's a genuine buy/sell board
 * rather than the price-stats stand-in /ahstats used to be.
 */
public final class AuctionHouseManager {

    public enum ListResult { SUCCESS, TOO_MANY_LISTINGS, INVALID_PRICE }
    public enum BuyResult { SUCCESS, NOT_FOUND, INSUFFICIENT_FUNDS, OWN_LISTING }

    private final Plugin plugin;
    private final DatabaseManager database;
    private final EconomyManager economy;
    private final AtomicInteger maxListingsPerPlayer;

    public AuctionHouseManager(Plugin plugin, DatabaseManager database, EconomyManager economy, int maxListingsPerPlayer) {
        this.plugin = plugin;
        this.database = database;
        this.economy = economy;
        this.maxListingsPerPlayer = new AtomicInteger(maxListingsPerPlayer);
    }

    public ListResult list(Player seller, ItemStack item, double price) {
        if (price <= 0) {
            return ListResult.INVALID_PRICE;
        }
        if (countBySeller(seller.getUniqueId()) >= maxListingsPerPlayer.get()) {
            return ListResult.TOO_MANY_LISTINGS;
        }
        String displayName = null;
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.getDisplayName() != null) {
            displayName = meta.getDisplayName();
        }
        String sql = "INSERT INTO listings (seller_id, seller_name, material, amount, display_name, price, listed_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, seller.getUniqueId().toString());
            statement.setString(2, seller.getName());
            statement.setString(3, item.getType().name());
            statement.setInt(4, item.getAmount());
            statement.setString(5, displayName);
            statement.setDouble(6, price);
            statement.setLong(7, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create auction house listing", ex);
        }
        return ListResult.SUCCESS;
    }

    public BuyResult buy(Player buyer, int listingId) {
        AuctionListing listing = find(listingId);
        if (listing == null) {
            return BuyResult.NOT_FOUND;
        }
        if (listing.sellerId().equals(buyer.getUniqueId())) {
            return BuyResult.OWN_LISTING;
        }
        if (!economy.has(buyer, listing.price())) {
            return BuyResult.INSUFFICIENT_FUNDS;
        }
        if (!delete(listingId)) {
            return BuyResult.NOT_FOUND;
        }
        economy.withdrawPlayer(buyer, listing.price());
        OfflinePlayer seller = Bukkit.getOfflinePlayer(listing.sellerId());
        economy.depositPlayer(seller, listing.price());
        giveItem(buyer, listing);
        Player onlineSeller = Bukkit.getPlayer(listing.sellerId());
        if (onlineSeller != null) {
            onlineSeller.sendMessage("§a" + buyer.getName() + " bought your listing #" + listing.id() + " for " + formatMoney(listing.price()) + ".");
        }
        return BuyResult.SUCCESS;
    }

    /** Cancels a listing and returns the item to the seller (if online). Used by both the seller's
     * own /orders cancel and staff's /shopedit remove. */
    public boolean cancel(int listingId, Player returnItemTo) {
        AuctionListing listing = find(listingId);
        if (listing == null || !delete(listingId)) {
            return false;
        }
        if (returnItemTo != null) {
            giveItem(returnItemTo, listing);
        }
        return true;
    }

    public AuctionListing find(int listingId) {
        String sql = "SELECT * FROM listings WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, listingId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? map(rows) : null;
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to look up listing #" + listingId, ex);
            return null;
        }
    }

    public List<AuctionListing> activeListings() {
        return query("SELECT * FROM listings ORDER BY listed_at DESC");
    }

    public List<AuctionListing> listingsBySeller(UUID sellerId) {
        String sql = "SELECT * FROM listings WHERE seller_id = ? ORDER BY listed_at DESC";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sellerId.toString());
            List<AuctionListing> results = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    results.add(map(rows));
                }
            }
            return results;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to look up listings for " + sellerId, ex);
            return List.of();
        }
    }

    private int countBySeller(UUID sellerId) {
        return listingsBySeller(sellerId).size();
    }

    private boolean delete(int listingId) {
        String sql = "DELETE FROM listings WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, listingId);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete listing #" + listingId, ex);
            return false;
        }
    }

    private List<AuctionListing> query(String sql) {
        List<AuctionListing> results = new ArrayList<>();
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                results.add(map(rows));
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to query listings", ex);
        }
        return results;
    }

    private AuctionListing map(ResultSet rows) throws SQLException {
        return new AuctionListing(
                rows.getInt("id"),
                UUID.fromString(rows.getString("seller_id")),
                rows.getString("seller_name"),
                Material.valueOf(rows.getString("material")),
                rows.getInt("amount"),
                rows.getString("display_name"),
                rows.getDouble("price"),
                rows.getLong("listed_at")
        );
    }

    private void giveItem(Player recipient, AuctionListing listing) {
        ItemStack item = new ItemStack(listing.material(), listing.amount());
        if (listing.displayName() != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(listing.displayName());
                item.setItemMeta(meta);
            }
        }
        var leftover = recipient.getInventory().addItem(item);
        if (leftover != null && !leftover.isEmpty()) {
            for (ItemStack overflow : leftover.values()) {
                recipient.getWorld().dropItem(recipient.getLocation(), overflow);
            }
            recipient.sendMessage("§eYour inventory was full - the rest was dropped at your feet.");
        }
    }

    private static String formatMoney(double amount) {
        return "$" + String.format(Locale.US, "%,.2f", amount);
    }
}
