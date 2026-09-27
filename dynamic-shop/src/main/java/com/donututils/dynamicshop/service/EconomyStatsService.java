package com.donututils.dynamicshop.service;

import com.donututils.dynamicshop.config.DynamicShopConfig;
import com.donututils.dynamicshop.engine.ShopItemRegistry;
import com.donututils.dynamicshop.model.ShopItem;
import com.donututils.dynamicshop.model.TransactionRecord;
import com.donututils.dynamicshop.storage.TransactionLogStore;

import java.util.function.Supplier;

/**
 * "GDP" here is buy+sell transaction volume in a trailing window - a measure of how active the shop
 * economy currently is - computed live from the transaction log rather than a separate periodic
 * snapshot. "Debt" is total outstanding loan principal. "Inflation" is the average current-price-to-
 * base-price ratio across every shop item, as a % deviation from each item's starting price.
 */
public final class EconomyStatsService {

    public record EconomyStats(double gdp, double totalDebt, double inflationPercent, long transactionCount) {
    }

    private final TransactionLogStore transactionLog;
    private final ShopItemRegistry itemRegistry;
    private final LoanService loanService;
    private final Supplier<DynamicShopConfig> configSupplier;

    public EconomyStatsService(TransactionLogStore transactionLog, ShopItemRegistry itemRegistry,
                                LoanService loanService, Supplier<DynamicShopConfig> configSupplier) {
        this.transactionLog = transactionLog;
        this.itemRegistry = itemRegistry;
        this.loanService = loanService;
        this.configSupplier = configSupplier;
    }

    public EconomyStats compute() {
        long windowMillis = configSupplier.get().gdpWindowHours() * 3_600_000L;
        long cutoff = System.currentTimeMillis() - windowMillis;
        double gdp = 0;
        long count = 0;
        for (TransactionRecord record : transactionLog.readAll()) {
            if (record.timestamp() < cutoff) {
                continue;
            }
            gdp += record.totalAmount();
            count++;
        }

        double debt = loanService.totalOutstandingDebt();

        double ratioSum = 0;
        int itemCount = 0;
        for (ShopItem item : itemRegistry.all()) {
            if (item.basePrice() <= 0) {
                continue;
            }
            ratioSum += item.currentPrice() / item.basePrice();
            itemCount++;
        }
        double inflationPercent = itemCount == 0 ? 0 : ((ratioSum / itemCount) - 1.0) * 100.0;

        return new EconomyStats(gdp, debt, inflationPercent, count);
    }
}
