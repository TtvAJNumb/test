package com.donututils.stockmarket.service;

import com.donututils.stockmarket.discord.StockAlerts;
import com.donututils.stockmarket.economy.VaultEconomyBridge;
import com.donututils.stockmarket.engine.StockRegistry;
import com.donututils.stockmarket.model.Holding;
import com.donututils.stockmarket.model.Stock;
import com.donututils.stockmarket.storage.HolderIndexStore;
import com.donututils.stockmarket.storage.PortfolioStore;

import java.util.Map;
import java.util.UUID;

/** Admin-only market operations: IPOs, delisting, splits, halts, and news events. */
public final class MarketAdminService {

    private final StockRegistry registry;
    private final PortfolioStore portfolioStore;
    private final HolderIndexStore holderIndex;
    private final VaultEconomyBridge economy;
    private final StockAlerts alerts;

    public MarketAdminService(StockRegistry registry, PortfolioStore portfolioStore, HolderIndexStore holderIndex,
                               VaultEconomyBridge economy, StockAlerts alerts) {
        this.registry = registry;
        this.portfolioStore = portfolioStore;
        this.holderIndex = holderIndex;
        this.economy = economy;
        this.alerts = alerts;
    }

    /** Returns an error message, or null on success. */
    public String createStock(String symbol, String name, String sector, double startingPrice,
                               long sharesOutstanding, double drift, double volatility) {
        if (registry.exists(symbol)) {
            return "A stock with symbol " + symbol.toUpperCase() + " already exists.";
        }
        if (startingPrice <= 0 || sharesOutstanding <= 0) {
            return "Starting price and shares outstanding must both be positive.";
        }
        Stock stock = new Stock(symbol, name, sector, startingPrice, sharesOutstanding, drift, volatility);
        registry.add(stock);
        alerts.ipo(stock);
        return null;
    }

    public String delist(String symbol, boolean payoutAtFinalPrice) {
        Stock stock = registry.get(symbol);
        if (stock == null) {
            return "Unknown stock: " + symbol;
        }
        double finalPrice = stock.price();
        stock.setDelisted(true);

        for (UUID holder : holderIndex.getHolders(stock.symbol())) {
            Map<String, Holding> portfolio = portfolioStore.load(holder);
            Holding holding = portfolio.remove(stock.symbol());
            if (holding != null && payoutAtFinalPrice) {
                economy.deposit(holder, holding.shares() * finalPrice);
            }
            portfolioStore.save(holder, portfolio);
        }
        holderIndex.clearAll(stock.symbol());

        alerts.delisted(stock);
        return null;
    }

    public String split(String symbol, double ratio) {
        Stock stock = registry.get(symbol);
        if (stock == null) {
            return "Unknown stock: " + symbol;
        }
        if (ratio <= 0) {
            return "Split ratio must be positive.";
        }

        stock.setPrice(stock.price() / ratio);
        stock.setPreviousClose(stock.previousClose() / ratio);
        stock.setDayOpen(stock.dayOpen() / ratio);
        stock.setDayHigh(stock.dayHigh() / ratio);
        stock.setDayLow(stock.dayLow() / ratio);
        stock.setSharesOutstanding((long) (stock.sharesOutstanding() * ratio));

        for (UUID holder : holderIndex.getHolders(stock.symbol())) {
            Map<String, Holding> portfolio = portfolioStore.load(holder);
            Holding holding = portfolio.get(stock.symbol());
            if (holding != null) {
                portfolio.put(stock.symbol(), new Holding((long) (holding.shares() * ratio), holding.costBasisTotal()));
                portfolioStore.save(holder, portfolio);
            }
        }

        alerts.split(stock, ratio);
        return null;
    }

    public String halt(String symbol, long durationSeconds) {
        Stock stock = registry.get(symbol);
        if (stock == null) {
            return "Unknown stock: " + symbol;
        }
        stock.halt(System.currentTimeMillis() + (durationSeconds * 1000L));
        return null;
    }

    public String resume(String symbol) {
        Stock stock = registry.get(symbol);
        if (stock == null) {
            return "Unknown stock: " + symbol;
        }
        stock.restoreHalt(false, 0);
        return null;
    }

    public String triggerEvent(String symbol, double driftBoost, double volatilityBoost, long durationSeconds) {
        Stock stock = registry.get(symbol);
        if (stock == null) {
            return "Unknown stock: " + symbol;
        }
        stock.applyEvent(driftBoost, volatilityBoost, System.currentTimeMillis() + (durationSeconds * 1000L));
        return null;
    }
}
