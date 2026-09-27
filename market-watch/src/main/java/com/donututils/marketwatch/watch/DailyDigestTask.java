package com.donututils.marketwatch.watch;

import com.donututils.marketwatch.config.MarketWatchConfig;
import com.donututils.marketwatch.discord.DiscordWebhook;
import com.donututils.marketwatch.service.MarketStatsService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Checked every few minutes rather than scheduled at an exact tick offset - simpler, and immune
 * to clock/tick drift over a long uptime. Only actually posts once per UTC calendar day, the first
 * time this runs at or after the configured hour.
 */
public final class DailyDigestTask implements Runnable {

    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    private final Supplier<MarketWatchConfig> configSupplier;
    private final MarketStatsService statsService;
    private final DiscordWebhook webhook;
    private long lastSentEpochDay = -1;

    public DailyDigestTask(Supplier<MarketWatchConfig> configSupplier, MarketStatsService statsService, DiscordWebhook webhook) {
        this.configSupplier = configSupplier;
        this.statsService = statsService;
        this.webhook = webhook;
    }

    @Override
    public void run() {
        MarketWatchConfig config = configSupplier.get();
        if (!config.dailyDigestEnabled() || !webhook.isConfigured()) {
            return;
        }

        Instant now = Instant.now();
        long currentEpochDay = LocalDate.ofInstant(now, ZoneOffset.UTC).toEpochDay();
        int currentHour = now.atZone(ZoneOffset.UTC).getHour();
        if (currentHour < config.dailyDigestHourUtc() || currentEpochDay == lastSentEpochDay) {
            return;
        }

        MarketStatsService.EconomyChange change = statsService.getEconomyChange(DAY_MILLIS);
        List<MarketStatsService.ItemStats> topItems = statsService.getTopTradedItems(DAY_MILLIS, 5);

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Total money", String.format(Locale.US, "$%,.2f", change.currentTotal()));
        fields.put("24h change", String.format(Locale.US, "%+.2f%%", change.percentChange()));
        fields.put("Players tracked", String.valueOf(change.playerCount()));
        if (!topItems.isEmpty()) {
            StringBuilder topList = new StringBuilder();
            for (MarketStatsService.ItemStats item : topItems) {
                if (topList.length() > 0) {
                    topList.append('\n');
                }
                topList.append(item.material()).append(" - ").append(item.volume()).append(" sold");
            }
            fields.put("Top traded (24h)", topList.toString());
        }

        webhook.sendAlert("Daily market report", "Economy and auction house summary for the last 24 hours.", 0x2ECC71, fields);
        lastSentEpochDay = currentEpochDay;
    }
}
