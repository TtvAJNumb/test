package com.donututils.realworld.stockmarket.service;

import com.donututils.realworld.stockmarket.config.StockMarketConfig;
import com.donututils.realworld.stockmarket.economy.VaultEconomyBridge;
import com.donututils.realworld.stockmarket.engine.StockRegistry;
import com.donututils.realworld.stockmarket.model.Holding;
import com.donututils.realworld.stockmarket.model.Stock;
import com.donututils.realworld.stockmarket.model.TransactionRecord;
import com.donututils.realworld.stockmarket.model.TransactionSide;
import com.donututils.realworld.stockmarket.storage.HolderIndexStore;
import com.donututils.realworld.stockmarket.storage.PortfolioStore;
import com.donututils.realworld.stockmarket.storage.TransactionLogStore;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Core buy/sell logic. Market orders only in v1 - fills instantly at the stock's current price.
 * Every fill nudges the stock's price via {@link Stock#addNetFlow} so real trading visibly moves
 * the market (see {@link com.donututils.realworld.stockmarket.engine.PriceEngine}).
 */
public final class TradingService {

    private final StockRegistry registry;
    private final VaultEconomyBridge economy;
    private final PortfolioStore portfolioStore;
    private final HolderIndexStore holderIndex;
    private final TransactionLogStore transactionLog;
    private final Supplier<StockMarketConfig> configSupplier;
    private final DividendPool dividendPool;
    private final Map<UUID, Long> lastTradeAtMillis = new ConcurrentHashMap<>();

    public TradingService(StockRegistry registry, VaultEconomyBridge economy, PortfolioStore portfolioStore,
                           HolderIndexStore holderIndex, TransactionLogStore transactionLog,
                           Supplier<StockMarketConfig> configSupplier, DividendPool dividendPool) {
        this.registry = registry;
        this.economy = economy;
        this.portfolioStore = portfolioStore;
        this.holderIndex = holderIndex;
        this.transactionLog = transactionLog;
        this.configSupplier = configSupplier;
        this.dividendPool = dividendPool;
    }

    public record TradeResult(boolean success, String message, TransactionRecord record) {
        static TradeResult failure(String message) {
            return new TradeResult(false, message, null);
        }

        static TradeResult ok(String message, TransactionRecord record) {
            return new TradeResult(true, message, record);
        }
    }

    public TradeResult buy(UUID playerId, String playerName, String symbol, long shares) {
        if (shares <= 0) {
            return TradeResult.failure("Enter a positive number of shares.");
        }
        Stock stock = registry.get(symbol);
        if (stock == null) {
            return TradeResult.failure("Unknown stock: " + symbol);
        }
        if (stock.delisted()) {
            return TradeResult.failure(stock.symbol() + " has been delisted and can no longer be traded.");
        }
        long now = System.currentTimeMillis();
        if (stock.haltActive(now)) {
            return TradeResult.failure(stock.symbol() + " is currently halted from trading.");
        }

        StockMarketConfig config = configSupplier.get();
        String rateLimitError = checkRateLimit(playerId, now, config);
        if (rateLimitError != null) {
            return TradeResult.failure(rateLimitError);
        }

        double price = stock.price();
        double gross = shares * price;
        double fee = gross * (config.brokerFeePercent() / 100.0);
        double total = gross + fee;

        if (!economy.has(playerId, total)) {
            return TradeResult.failure("You don't have enough money - need $" + fmt(total) + " (including fee).");
        }

        Map<String, Holding> portfolio = portfolioStore.load(playerId);
        Holding current = portfolio.getOrDefault(stock.symbol(), Holding.NONE);
        long newShareCount = current.shares() + shares;
        double maxPercent = config.maxSharesPerPlayerPercent();
        if (maxPercent > 0) {
            long maxShares = (long) (stock.sharesOutstanding() * (maxPercent / 100.0));
            if (newShareCount > maxShares) {
                return TradeResult.failure("You can't own more than " + maxShares + " shares of " + stock.symbol() + " (ownership cap).");
            }
        }

        VaultEconomyBridge.EconomyResult withdrawal = economy.withdraw(playerId, total);
        if (!withdrawal.success()) {
            return TradeResult.failure("Payment failed: " + (withdrawal.errorMessage() == null ? "unknown error" : withdrawal.errorMessage()));
        }

        Holding updated = current.withBuy(shares, gross);
        portfolio.put(stock.symbol(), updated);
        portfolioStore.save(playerId, portfolio);
        holderIndex.addHolder(stock.symbol(), playerId);

        stock.addVolume(shares);
        stock.addNetFlow(gross);
        dividendPool.credit(fee);

        TransactionRecord record = new TransactionRecord(now, playerId, playerName, stock.symbol(), TransactionSide.BUY, shares, price, fee);
        transactionLog.append(record);
        lastTradeAtMillis.put(playerId, now);

        return TradeResult.ok("Bought " + shares + " share(s) of " + stock.symbol() + " at $" + fmt(price)
                + " each (fee $" + fmt(fee) + ", total $" + fmt(total) + ").", record);
    }

    public TradeResult sell(UUID playerId, String playerName, String symbol, long shares) {
        if (shares <= 0) {
            return TradeResult.failure("Enter a positive number of shares.");
        }
        Stock stock = registry.get(symbol);
        if (stock == null) {
            return TradeResult.failure("Unknown stock: " + symbol);
        }
        long now = System.currentTimeMillis();
        if (!stock.delisted() && stock.haltActive(now)) {
            return TradeResult.failure(stock.symbol() + " is currently halted from trading.");
        }

        StockMarketConfig config = configSupplier.get();
        String rateLimitError = checkRateLimit(playerId, now, config);
        if (rateLimitError != null) {
            return TradeResult.failure(rateLimitError);
        }

        Map<String, Holding> portfolio = portfolioStore.load(playerId);
        Holding current = portfolio.getOrDefault(stock.symbol(), Holding.NONE);
        if (current.shares() < shares) {
            return TradeResult.failure("You only own " + current.shares() + " share(s) of " + stock.symbol() + ".");
        }

        double price = stock.price();
        double gross = shares * price;
        double fee = gross * (config.brokerFeePercent() / 100.0);
        double proceeds = gross - fee;

        VaultEconomyBridge.EconomyResult deposit = economy.deposit(playerId, proceeds);
        if (!deposit.success()) {
            return TradeResult.failure("Payment failed: " + (deposit.errorMessage() == null ? "unknown error" : deposit.errorMessage()));
        }

        Holding updated = current.withSell(shares);
        if (updated.isEmpty()) {
            portfolio.remove(stock.symbol());
            holderIndex.removeHolder(stock.symbol(), playerId);
        } else {
            portfolio.put(stock.symbol(), updated);
        }
        portfolioStore.save(playerId, portfolio);

        stock.addVolume(shares);
        stock.addNetFlow(-gross);
        dividendPool.credit(fee);

        TransactionRecord record = new TransactionRecord(now, playerId, playerName, stock.symbol(), TransactionSide.SELL, shares, price, fee);
        transactionLog.append(record);
        lastTradeAtMillis.put(playerId, now);

        return TradeResult.ok("Sold " + shares + " share(s) of " + stock.symbol() + " at $" + fmt(price)
                + " each (fee $" + fmt(fee) + ", proceeds $" + fmt(proceeds) + ").", record);
    }

    private String checkRateLimit(UUID playerId, long now, StockMarketConfig config) {
        if (config.tradeRateLimitMillis() <= 0) {
            return null;
        }
        Long last = lastTradeAtMillis.get(playerId);
        if (last != null && now - last < config.tradeRateLimitMillis()) {
            return "You're trading too fast - wait a moment before your next trade.";
        }
        return null;
    }

    private static String fmt(double value) {
        return String.format(Locale.US, "%,.2f", value);
    }
}
