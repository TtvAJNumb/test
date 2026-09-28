package com.donututils.realworld.stockmarket.service;

import com.donututils.realworld.stockmarket.config.StockMarketConfig;
import com.donututils.realworld.stockmarket.discord.StockAlerts;
import com.donututils.realworld.stockmarket.engine.StockRegistry;
import com.donututils.realworld.stockmarket.model.Stock;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Supplier;

/**
 * Once-a-day (UTC) market-wide summary: an unweighted index level (average stock price) and its
 * day-over-day change, plus the day's top gainer/loser. Checked periodically rather than scheduled
 * at an exact tick offset - simpler, and immune to clock/tick drift over a long uptime.
 */
public final class MarketSummaryTask implements Runnable {

    private final StockRegistry registry;
    private final StockAlerts alerts;
    private final Supplier<StockMarketConfig> configSupplier;
    private long lastSentEpochDay = -1;

    public MarketSummaryTask(StockRegistry registry, StockAlerts alerts, Supplier<StockMarketConfig> configSupplier) {
        this.registry = registry;
        this.alerts = alerts;
        this.configSupplier = configSupplier;
    }

    @Override
    public void run() {
        StockMarketConfig config = configSupplier.get();
        if (!config.dailySummaryEnabled()) {
            return;
        }

        Instant now = Instant.now();
        long currentEpochDay = LocalDate.ofInstant(now, ZoneOffset.UTC).toEpochDay();
        int currentHour = now.atZone(ZoneOffset.UTC).getHour();
        if (currentHour < config.dailySummaryHourUtc() || currentEpochDay == lastSentEpochDay) {
            return;
        }

        List<Stock> stocks = registry.allActive().stream().toList();
        if (stocks.isEmpty()) {
            lastSentEpochDay = currentEpochDay;
            return;
        }

        double totalPrice = 0;
        double totalChange = 0;
        Stock topGainer = null;
        Stock topLoser = null;
        for (Stock stock : stocks) {
            totalPrice += stock.price();
            totalChange += stock.dayChangePercent();
            if (topGainer == null || stock.dayChangePercent() > topGainer.dayChangePercent()) {
                topGainer = stock;
            }
            if (topLoser == null || stock.dayChangePercent() < topLoser.dayChangePercent()) {
                topLoser = stock;
            }
        }

        double indexLevel = totalPrice / stocks.size();
        double indexChange = totalChange / stocks.size();
        String gainerText = topGainer == null ? null
                : topGainer.symbol() + " (" + String.format("%+.2f%%", topGainer.dayChangePercent()) + ")";
        String loserText = topLoser == null ? null
                : topLoser.symbol() + " (" + String.format("%+.2f%%", topLoser.dayChangePercent()) + ")";

        alerts.dailySummary(indexLevel, indexChange, gainerText, loserText);
        lastSentEpochDay = currentEpochDay;
    }
}
