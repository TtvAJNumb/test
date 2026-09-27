package com.donututils.stockmarket.service;

import com.donututils.stockmarket.economy.VaultEconomyBridge;
import com.donututils.stockmarket.engine.StockRegistry;
import com.donututils.stockmarket.model.Holding;
import com.donututils.stockmarket.model.Stock;
import com.donututils.stockmarket.storage.HolderIndexStore;
import com.donututils.stockmarket.storage.PortfolioStore;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Checked periodically rather than scheduled at an exact offset (same reasoning as MarketWatch's
 * daily digest) - pays out whenever enough time has passed since a dividend-paying stock's last
 * payout, to every player currently holding shares (via the holder index, so offline players still
 * get paid - Vault's economy interface works on OfflinePlayer).
 */
public final class DividendService implements Runnable {

    private final StockRegistry registry;
    private final HolderIndexStore holderIndex;
    private final PortfolioStore portfolioStore;
    private final VaultEconomyBridge economy;
    private final Logger logger;

    public DividendService(StockRegistry registry, HolderIndexStore holderIndex, PortfolioStore portfolioStore,
                            VaultEconomyBridge economy, Logger logger) {
        this.registry = registry;
        this.holderIndex = holderIndex;
        this.portfolioStore = portfolioStore;
        this.economy = economy;
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

        for (UUID holderId : holderIndex.getHolders(stock.symbol())) {
            Holding holding = portfolioStore.load(holderId).get(stock.symbol());
            if (holding == null || holding.isEmpty()) {
                continue;
            }
            double payout = holding.shares() * perShare;
            VaultEconomyBridge.EconomyResult result = economy.deposit(holderId, payout);
            if (!result.success()) {
                logger.log(Level.WARNING, "Failed to pay dividend to " + holderId + " for " + stock.symbol()
                        + ": " + result.errorMessage());
            }
        }

        stock.setLastDividendAtMillis(now);
    }
}
