package com.donututils.realworld.stockmarket.config;

public record StockMarketConfig(
        long tickIntervalSeconds,
        long tradingDayLengthMinutes,

        double maxTradeImpactPercentPerTick,
        double circuitBreakerThresholdPercent,
        long circuitBreakerCooldownSeconds,

        double brokerFeePercent,
        double maxSharesPerPlayerPercent,
        long tradeRateLimitMillis,

        double bigMoverAlertThresholdPercent,

        String webhookUrl,
        String webhookUsername,
        boolean alertBigMover,
        boolean alertCircuitBreaker,
        boolean alertIpo,
        boolean alertSplit,
        boolean alertDelisting,
        boolean dailySummaryEnabled,
        int dailySummaryHourUtc
) {
    public long tradingDayLengthMillis() {
        return tradingDayLengthMinutes * 60_000L;
    }
}
