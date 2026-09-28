package com.donututils.realworld.stockmarket.service;

import com.donututils.realworld.stockmarket.economy.VaultEconomyBridge;
import com.donututils.realworld.stockmarket.engine.StockRegistry;
import com.donututils.realworld.stockmarket.model.Holding;
import com.donututils.realworld.stockmarket.model.Stock;
import com.donututils.realworld.stockmarket.storage.PortfolioStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PortfolioService {

    private final StockRegistry registry;
    private final PortfolioStore portfolioStore;
    private final VaultEconomyBridge economy;

    public PortfolioService(StockRegistry registry, PortfolioStore portfolioStore, VaultEconomyBridge economy) {
        this.registry = registry;
        this.portfolioStore = portfolioStore;
        this.economy = economy;
    }

    public record PositionView(String symbol, long shares, double averageCost, double currentPrice,
                                double marketValue, double unrealizedPnl, double unrealizedPnlPercent) {
    }

    public record PortfolioView(double cash, double holdingsValue, double totalValue, List<PositionView> positions) {
    }

    public PortfolioView getPortfolio(UUID playerId) {
        Map<String, Holding> holdings = portfolioStore.load(playerId);
        List<PositionView> positions = new ArrayList<>();
        double holdingsValue = 0;

        for (Map.Entry<String, Holding> entry : holdings.entrySet()) {
            Stock stock = registry.get(entry.getKey());
            Holding holding = entry.getValue();
            double currentPrice = stock == null ? 0 : stock.price();
            double marketValue = holding.shares() * currentPrice;
            double pnl = marketValue - holding.costBasisTotal();
            double pnlPercent = holding.costBasisTotal() == 0 ? 0 : (pnl / holding.costBasisTotal()) * 100.0;
            holdingsValue += marketValue;
            positions.add(new PositionView(entry.getKey(), holding.shares(), holding.averageCost(),
                    currentPrice, marketValue, pnl, pnlPercent));
        }

        double cash = economy.getBalance(playerId);
        return new PortfolioView(cash, holdingsValue, cash + holdingsValue, positions);
    }

    public double totalNetWorth(UUID playerId) {
        return getPortfolio(playerId).totalValue();
    }

    /** Null if the player doesn't hold that symbol. */
    public PositionView getPosition(UUID playerId, String symbol) {
        for (PositionView position : getPortfolio(playerId).positions()) {
            if (position.symbol().equalsIgnoreCase(symbol)) {
                return position;
            }
        }
        return null;
    }
}
