package com.donututils.dynamicshop.config;

import java.util.List;

/** Immutable snapshot of config.yml, re-read on every reload. */
public record DynamicShopConfig(
        int tickIntervalSeconds,
        int autosaveIntervalSeconds,
        boolean moneyEnabled,
        String moneyDisplayName,
        boolean shardsEnabled,
        String shardsDisplayName,
        String guiTitle,
        boolean autoSellFeatureEnabled,
        boolean loansFeatureEnabled,
        boolean webServerFeatureEnabled,
        boolean gdpStatsFeatureEnabled,
        boolean tutorialFeatureEnabled,
        boolean purchaseRestrictionEnabled,
        int transactionCooldownSeconds,
        int globalMaxQuantityPerTransaction,
        double maxLoanAmount,
        double interestRatePercentPerDay,
        int maxOutstandingLoansPerPlayer,
        String httpBindAddress,
        int httpPort,
        String httpApiKey,
        int gdpSnapshotIntervalMinutes,
        List<String> tutorialLines
) {

    public long tickIntervalMillis() {
        return tickIntervalSeconds * 1000L;
    }
}
