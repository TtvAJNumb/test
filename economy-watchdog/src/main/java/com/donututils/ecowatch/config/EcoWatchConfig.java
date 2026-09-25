package com.donututils.ecowatch.config;

public record EcoWatchConfig(
        String webhookUrl,
        String webhookUsername,
        int pollIntervalSeconds,

        boolean balanceJumpEnabled,
        double minAbsoluteDelta,
        double minPercentDelta,

        boolean largeTransferEnabled,
        double minTransferAmount,

        boolean auctionEnabled,
        double highPriceFlag,
        double lowPriceFlag,
        boolean repeatTradingEnabled,
        int repeatWindowMinutes,
        int repeatMinTrades
) {
}
