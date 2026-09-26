package com.donututils.marketwatch;

import com.donututils.marketwatch.command.AhStatsCommand;
import com.donututils.marketwatch.command.MarketWatchCommand;
import com.donututils.marketwatch.config.MarketWatchConfig;
import com.donututils.marketwatch.discord.DiscordWebhook;
import com.donututils.marketwatch.http.MarketHttpServer;
import com.donututils.marketwatch.reflect.UdsBridge;
import com.donututils.marketwatch.service.MarketStatsService;
import com.donututils.marketwatch.storage.AuctionSaleStore;
import com.donututils.marketwatch.storage.EconomySnapshotStore;
import com.donututils.marketwatch.watch.AuctionSaleWatcher;
import com.donututils.marketwatch.watch.DailyDigestTask;
import com.donututils.marketwatch.watch.EconomySnapshotTask;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public final class MarketWatchPlugin extends JavaPlugin {

    private static final long PRUNE_INTERVAL_TICKS = 24L * 60L * 60L * 20L;
    private static final long DIGEST_CHECK_INTERVAL_TICKS = 10L * 60L * 20L;

    private UdsBridge bridge;
    private EconomySnapshotStore economyStore;
    private AuctionSaleStore auctionStore;
    private MarketStatsService statsService;
    private DiscordWebhook webhook;
    private MarketHttpServer httpServer;
    private volatile MarketWatchConfig config;

    private BukkitTask economyTask;
    private BukkitTask auctionTask;
    private BukkitTask digestTask;
    private BukkitTask pruneTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Plugin uds = getServer().getPluginManager().getPlugin("UltimateDonutSmp");
        if (uds == null || !uds.isEnabled()) {
            getLogger().severe("UltimateDonutSmp is not enabled. Disabling MarketWatch.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            bridge = UdsBridge.create(uds);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            getLogger().severe("This UltimateDonutSmp version does not expose the leaderboard/auction API this add-on needs: " + ex);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        economyStore = new EconomySnapshotStore(getDataFolder(), getLogger());
        auctionStore = new AuctionSaleStore(getDataFolder(), getLogger());
        statsService = new MarketStatsService(economyStore, auctionStore);
        webhook = new DiscordWebhook(this);
        httpServer = new MarketHttpServer(this, this::getMarketWatchConfig, economyStore, statsService);

        setExecutorIfPresent("ahstats", new AhStatsCommand(statsService));
        setExecutorIfPresent("marketwatch", new MarketWatchCommand(this));

        startWatching();
    }

    @Override
    public void onDisable() {
        stopWatching();
    }

    public void reloadMarketWatch() {
        reloadConfig();
        stopWatching();
        startWatching();
    }

    public boolean isHttpEnabled() {
        return config != null && config.httpEnabled();
    }

    public boolean isWebhookConfigured() {
        return webhook != null && webhook.isConfigured();
    }

    private void startWatching() {
        config = loadConfigValues();
        webhook.configure(config.webhookUrl(), config.webhookUsername());
        httpServer.start();

        long economyTicks = Math.max(20L, config.economySnapshotIntervalSeconds() * 20L);
        long auctionTicks = Math.max(20L, config.auctionPollIntervalSeconds() * 20L);

        economyTask = getServer().getScheduler().runTaskTimerAsynchronously(
                this, new EconomySnapshotTask(this, bridge, economyStore), economyTicks, economyTicks);
        auctionTask = getServer().getScheduler().runTaskTimerAsynchronously(
                this, new AuctionSaleWatcher(this, bridge, auctionStore, this::getMarketWatchConfig), auctionTicks, auctionTicks);
        digestTask = getServer().getScheduler().runTaskTimerAsynchronously(
                this, new DailyDigestTask(this::getMarketWatchConfig, statsService, webhook),
                DIGEST_CHECK_INTERVAL_TICKS, DIGEST_CHECK_INTERVAL_TICKS);
        pruneTask = getServer().getScheduler().runTaskTimerAsynchronously(this, this::pruneOldData,
                PRUNE_INTERVAL_TICKS, PRUNE_INTERVAL_TICKS);

        getLogger().info("MarketWatch enabled - economy snapshots every " + config.economySnapshotIntervalSeconds()
                + "s, auction polling every " + config.auctionPollIntervalSeconds() + "s.");
    }

    private void stopWatching() {
        cancel(economyTask);
        cancel(auctionTask);
        cancel(digestTask);
        cancel(pruneTask);
        economyTask = null;
        auctionTask = null;
        digestTask = null;
        pruneTask = null;
        if (httpServer != null) {
            httpServer.stop();
        }
    }

    private void cancel(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }

    private void pruneOldData() {
        MarketWatchConfig current = config;
        if (current == null) {
            return;
        }
        long now = System.currentTimeMillis();
        economyStore.pruneOlderThan(now - current.economyRetentionDays() * 24L * 60L * 60L * 1000L);
        auctionStore.pruneOlderThan(now - current.auctionRetentionDays() * 24L * 60L * 60L * 1000L);
    }

    private MarketWatchConfig getMarketWatchConfig() {
        return config;
    }

    private void setExecutorIfPresent(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        } else {
            getLogger().warning("plugin.yml is missing the '" + name + "' command definition.");
        }
    }

    private MarketWatchConfig loadConfigValues() {
        FileConfiguration c = getConfig();
        List<String> tracked = c.getStringList("auction.tracked-materials");
        return new MarketWatchConfig(
                Math.max(60, c.getInt("economy.snapshot-interval-seconds", 1800)),
                Math.max(1, c.getInt("economy.retention-days", 90)),
                Math.max(5, c.getInt("auction.poll-interval-seconds", 30)),
                Math.max(1, c.getInt("auction.retention-days", 30)),
                tracked == null ? List.of() : tracked,
                c.getBoolean("http.enabled", false),
                c.getInt("http.port", 8787),
                c.getString("http.bind-address", "127.0.0.1"),
                c.getString("http.api-key", ""),
                c.getString("discord.webhook-url", ""),
                c.getString("discord.username", "MarketWatch"),
                c.getBoolean("discord.daily-digest.enabled", false),
                c.getInt("discord.daily-digest.hour-of-day-utc", 12)
        );
    }
}
