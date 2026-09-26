package com.donututils.marketwatch.config;

import java.util.List;

public record MarketWatchConfig(
        long economySnapshotIntervalSeconds,
        int economyRetentionDays,
        long auctionPollIntervalSeconds,
        int auctionRetentionDays,
        List<String> trackedMaterials,
        boolean httpEnabled,
        int httpPort,
        String httpBindAddress,
        String httpApiKey,
        String webhookUrl,
        String webhookUsername,
        boolean dailyDigestEnabled,
        int dailyDigestHourUtc
) {
    public boolean isMaterialTracked(String material) {
        return trackedMaterials.isEmpty() || trackedMaterials.contains(material);
    }
}
