package com.donututils.ledger.stock;

import com.donututils.ledger.LedgerPlugin;
import com.donututils.ledger.config.LedgerConfig;
import com.donututils.ledger.corp.CorporationManager;
import com.donututils.ledger.db.DatabaseManager;
import com.donututils.ledger.economy.LedgerEconomyProvider;
import com.donututils.ledger.model.Corporation;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Buying/selling corporation shares, the baseline price-drift tick, and dividend payouts.
 * Corporation identity/financials live in {@link CorporationManager}; this owns the market side.
 */
public final class ShareTradingManager {

    public record TradeResult(boolean success, String message) {
    }

    private final LedgerPlugin plugin;
    private final DatabaseManager database;
    private final LedgerEconomyProvider economy;
    private final CorporationManager corporationManager;
    private final Supplier<LedgerConfig> configSupplier;
    private final Random random = new Random();

    public ShareTradingManager(LedgerPlugin plugin, DatabaseManager database, LedgerEconomyProvider economy,
                                CorporationManager corporationManager, Supplier<LedgerConfig> configSupplier) {
        this.plugin = plugin;
        this.database = database;
        this.economy = economy;
        this.corporationManager = corporationManager;
        this.configSupplier = configSupplier;
    }

    public TradeResult buy(OfflinePlayer buyer, Corporation corp, int quantity) {
        if (quantity <= 0) {
            return new TradeResult(false, "Quantity must be positive.");
        }
        double cost = corp.sharePrice() * quantity;
        EconomyResponse withdrawal = economy.withdrawPlayer(buyer, cost);
        if (!withdrawal.transactionSuccess()) {
            return new TradeResult(false, withdrawal.errorMessage != null ? withdrawal.errorMessage : "You don't have enough.");
        }
        int newShares = corporationManager.sharesHeldBy(corp.id(), buyer.getUniqueId()) + quantity;
        corporationManager.setShares(corp.id(), buyer.getUniqueId(), newShares);
        corporationManager.upsertShareholding(corp.id(), buyer.getUniqueId(), newShares);
        corp.addNetFlow(cost);
        logTransaction(corp.id(), buyer.getUniqueId(), "BUY", quantity, corp.sharePrice());
        return new TradeResult(true, String.format(Locale.US, "Bought %d share(s) of %s for %s.", quantity, corp.ticker(), economy.format(cost)));
    }

    public TradeResult sell(OfflinePlayer seller, Corporation corp, int quantity) {
        if (quantity <= 0) {
            return new TradeResult(false, "Quantity must be positive.");
        }
        int owned = corporationManager.sharesHeldBy(corp.id(), seller.getUniqueId());
        if (owned < quantity) {
            return new TradeResult(false, "You only own " + owned + " share(s) of " + corp.ticker() + ".");
        }
        double proceeds = corp.sharePrice() * quantity;
        economy.depositPlayer(seller, proceeds);
        int newShares = owned - quantity;
        corporationManager.setShares(corp.id(), seller.getUniqueId(), newShares);
        corporationManager.upsertShareholding(corp.id(), seller.getUniqueId(), newShares);
        corp.addNetFlow(-proceeds);
        logTransaction(corp.id(), seller.getUniqueId(), "SELL", quantity, corp.sharePrice());
        return new TradeResult(true, String.format(Locale.US, "Sold %d share(s) of %s for %s.", quantity, corp.ticker(), economy.format(proceeds)));
    }

    public TradeResult payDividend(OfflinePlayer founder, Corporation corp, double totalAmount) {
        if (!corp.founderId().equals(founder.getUniqueId())) {
            return new TradeResult(false, "Only " + corp.name() + "'s founder can pay a dividend.");
        }
        if (totalAmount <= 0 || totalAmount > corp.treasuryBalance()) {
            return new TradeResult(false, String.format(Locale.US, "%s's treasury only has %s.", corp.name(), economy.format(corp.treasuryBalance())));
        }
        Map<UUID, Integer> holders = corporationManager.shareholdersOf(corp.id());
        for (Map.Entry<UUID, Integer> entry : holders.entrySet()) {
            double share = totalAmount * entry.getValue() / (double) corp.totalShares();
            if (share > 0) {
                economy.depositPlayer(Bukkit.getOfflinePlayer(entry.getKey()), share);
            }
        }
        corp.setTreasuryBalance(corp.treasuryBalance() - totalAmount);
        corporationManager.updateCorporation(corp);
        return new TradeResult(true, String.format(Locale.US, "Paid out %s in dividends to %d shareholder(s).", economy.format(totalAmount), holders.size()));
    }

    /** Runs on a repeating async task: baseline random-walk drift/volatility (same idea as
     * StockMarket) plus whatever real buy/sell pressure accumulated since the last tick. */
    public void tickPrices() {
        LedgerConfig config = configSupplier.get();
        for (Corporation corp : corporationManager.all()) {
            double drift = config.corpBaseDrift();
            double volatility = config.corpBaseVolatility();
            double z = random.nextGaussian();
            double walkFactor = Math.exp((drift - 0.5 * volatility * volatility) + volatility * z);
            double price = corp.sharePrice() * walkFactor;

            double netFlow = corp.takePendingNetFlow();
            if (netFlow != 0) {
                double marketCap = Math.max(1.0, corp.marketCap());
                double impact = Math.max(-0.1, Math.min(0.1, netFlow / marketCap));
                price *= (1 + impact);
            }

            corp.setSharePrice(price);
            corporationManager.updateCorporation(corp);
        }
    }

    private void logTransaction(long corpId, UUID playerId, String side, int shares, double pricePerShare) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO share_transactions (corporation_id, player_id, side, shares, price_per_share, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, corpId);
                statement.setString(2, playerId.toString());
                statement.setString(3, side);
                statement.setInt(4, shares);
                statement.setDouble(5, pricePerShare);
                statement.setLong(6, System.currentTimeMillis());
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to log share transaction", ex);
            }
        });
    }
}
