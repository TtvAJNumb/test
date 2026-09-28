package com.donututils.stockmarket.service;

import com.donututils.stockmarket.economy.VaultEconomyBridge;
import com.donututils.stockmarket.engine.StockRegistry;
import com.donututils.stockmarket.model.Holding;
import com.donututils.stockmarket.model.Stock;
import com.donututils.stockmarket.storage.HolderIndexStore;
import com.donututils.stockmarket.storage.PortfolioStore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Checked periodically rather than scheduled at an exact offset (same reasoning as MarketWatch's
 * daily digest) - pays out whenever enough time has passed since a dividend-paying stock's last
 * payout, to every player currently holding shares (via the holder index, so offline players still
 * get paid - Vault's economy interface works on OfflinePlayer).
 * <p>
 * Payouts are funded from {@link DividendPool}, which only holds real broker-fee revenue collected
 * from actual trades - dividends never deposit money that wasn't first paid in by someone. If the
 * pool can't cover a stock's full desired payout, every holder's share is scaled down proportionally
 * rather than paying some holders in full and others nothing.
 */
public final class DividendService implements Runnable {

    private final StockRegistry registry;
    private final HolderIndexStore holderIndex;
    private final PortfolioStore portfolioStore;
    private final VaultEconomyBridge economy;
    private final DividendPool dividendPool;
    private final Logger logger;

    public DividendService(StockRegistry registry, HolderIndexStore holderIndex, PortfolioStore portfolioStore,
                            VaultEconomyBridge economy, DividendPool dividendPool, Logger logger) {
        this.registry = registry;
        this.holderIndex = holderIndex;
        this.portfolioStore = portfolioStore;
        this.economy = economy;
        this.dividendPool = dividendPool;
        this.logger = logger;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        for (Stock stock : registry.allActive()) {
            if (!stock.paysDividends()) {
                continue;
            }
            if (now - stock.lastDividendAtMillis() < stock.dividendIntervalMillis()) {
                continue;
            }
            payDividend(stock, now);
        }
    }

    private void payDividend(Stock stock, long now) {
        double perShare = stock.price() * stock.dividendYieldPerPayout();
        if (perShare <= 0) {
            stock.setLastDividendAtMillis(now);
            return;
        }

        Map<UUID, Double> desiredPayouts = new LinkedHashMap<>();
        double desiredTotal = 0;
        for (UUID holderId : holderIndex.getHolders(stock.symbol())) {
            Holding holding = portfolioStore.load(holderId).get(stock.symbol());
            if (holding == null || holding.isEmpty()) {
                continue;
            }
            double payout = holding.shares() * perShare;
            desiredPayouts.put(holderId, payout);
            desiredTotal += payout;
        }

        stock.setLastDividendAtMillis(now);
        if (desiredTotal <= 0) {
            return;
        }

        // Only pay out what the pool actually has (real fee revenue) - if it can't cover the full
        // desired amount, everyone's share is scaled down by the same ratio.
        double funded = dividendPool.debit(desiredTotal);
        double scale = funded / desiredTotal;

        for (Map.Entry<UUID, Double> entry : desiredPayouts.entrySet()) {
            double payout = entry.getValue() * scale;
            if (payout <= 0) {
                continue;
            }
            VaultEconomyBridge.EconomyResult result = economy.deposit(entry.getKey(), payout);
            if (!result.success()) {
                logger.log(Level.WARNING, "Failed to pay dividend to " + entry.getKey() + " for " + stock.symbol()
                        + ": " + result.errorMessage());
            }
        }
    }
}
