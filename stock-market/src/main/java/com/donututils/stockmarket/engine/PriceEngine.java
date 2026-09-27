package com.donututils.stockmarket.engine;

import com.donututils.stockmarket.config.StockMarketConfig;
import com.donututils.stockmarket.discord.StockAlerts;
import com.donututils.stockmarket.model.PriceTick;
import com.donututils.stockmarket.model.Stock;
import com.donututils.stockmarket.storage.PriceHistoryStore;

import java.util.Random;
import java.util.function.Supplier;

/**
 * Ticks every active stock on a schedule. Each tick is a geometric-Brownian-motion random walk
 * (drift + volatility) plus a small, capped nudge from real buy/sell pressure accumulated since the
 * last tick (see {@link Stock#addNetFlow}) - real trading visibly moves the price, but no single
 * trade (or even a whole burst of them) can move it more than {@code maxTradeImpactPercentPerTick}
 * in one tick. A circuit breaker separately caps how far the *combined* random-walk-plus-pressure
 * move can go before halting the stock, mirroring how real exchanges pause a stock that's crashing
 * or spiking too fast.
 */
public final class PriceEngine {

    private final StockRegistry registry;
    private final PriceHistoryStore historyStore;
    private final StockAlerts alerts;
    private final Supplier<StockMarketConfig> configSupplier;
    private final Random random = new Random();

    public PriceEngine(StockRegistry registry, PriceHistoryStore historyStore, StockAlerts alerts,
                        Supplier<StockMarketConfig> configSupplier) {
        this.registry = registry;
        this.historyStore = historyStore;
        this.alerts = alerts;
        this.configSupplier = configSupplier;
    }

    public void tick() {
        long now = System.currentTimeMillis();
        StockMarketConfig config = configSupplier.get();

        for (Stock stock : registry.allActive()) {
            maybeRolloverDay(stock, now, config);

            // Let an expired halt clear, and let an expired news event's boosts reset, even if we
            // don't move the price this tick because it's still halted.
            boolean stillHalted = stock.haltActive(now);
            stock.hasActiveEvent(now);
            if (stillHalted) {
                continue;
            }

            double oldPrice = stock.price();
            double newPrice = randomWalk(stock);
            newPrice = applyTradePressure(stock, newPrice, config);

            double changePercent = oldPrice == 0 ? 0 : ((newPrice - oldPrice) / oldPrice) * 100.0;
            double breakerThreshold = config.circuitBreakerThresholdPercent();
            if (Math.abs(changePercent) > breakerThreshold) {
                double sign = Math.signum(changePercent);
                newPrice = oldPrice * (1 + (sign * breakerThreshold / 100.0));
                stock.halt(now + (config.circuitBreakerCooldownSeconds() * 1000L));
                alerts.circuitBreaker(stock, changePercent);
            }

            stock.setPrice(newPrice);
            stock.setDayHigh(Math.max(stock.dayHigh(), newPrice));
            stock.setDayLow(Math.min(stock.dayLow(), newPrice));

            historyStore.append(stock.symbol(), new PriceTick(
                    now, oldPrice, Math.max(oldPrice, newPrice), Math.min(oldPrice, newPrice), newPrice, stock.volumeToday()));

            double recomputedChange = oldPrice == 0 ? 0 : ((newPrice - oldPrice) / oldPrice) * 100.0;
            if (Math.abs(recomputedChange) >= config.bigMoverAlertThresholdPercent()) {
                alerts.bigMover(stock, recomputedChange);
            }
        }
    }

    private double randomWalk(Stock stock) {
        double drift = stock.effectiveDrift();
        double volatility = stock.effectiveVolatility();
        double z = random.nextGaussian();
        double factor = Math.exp((drift - 0.5 * volatility * volatility) + volatility * z);
        return stock.price() * factor;
    }

    private double applyTradePressure(Stock stock, double price, StockMarketConfig config) {
        double netFlow = stock.takePendingNetFlow();
        if (netFlow == 0) {
            return price;
        }
        double marketCapReference = Math.max(1.0, stock.marketCap());
        double rawImpact = netFlow / marketCapReference;
        double cap = config.maxTradeImpactPercentPerTick() / 100.0;
        double clampedImpact = Math.max(-cap, Math.min(cap, rawImpact));
        return price * (1 + clampedImpact);
    }

    private void maybeRolloverDay(Stock stock, long now, StockMarketConfig config) {
        if (now - stock.dayStartMillis() >= config.tradingDayLengthMillis()) {
            stock.rolloverDay(now);
        }
    }
}
