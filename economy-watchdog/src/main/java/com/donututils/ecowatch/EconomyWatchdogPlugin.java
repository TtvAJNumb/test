package com.donututils.ecowatch;

import com.donututils.ecowatch.command.EcoWatchCommand;
import com.donututils.ecowatch.config.EcoWatchConfig;
import com.donututils.ecowatch.discord.DiscordWebhook;
import com.donututils.ecowatch.reflect.UdsBridge;
import com.donututils.ecowatch.watch.AuctionWatcher;
import com.donututils.ecowatch.watch.BalanceWatcher;
import com.donututils.ecowatch.watch.PayCommandWatcher;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class EconomyWatchdogPlugin extends JavaPlugin {

    private UdsBridge bridge;
    private DiscordWebhook webhook;
    private PayCommandWatcher payWatcher;
    private BukkitTask balanceTask;
    private BukkitTask auctionTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Plugin uds = getServer().getPluginManager().getPlugin("UltimateDonutSmp");
        if (uds == null || !uds.isEnabled()) {
            getLogger().severe("UltimateDonutSmp is not enabled. Disabling EconomyWatchdog.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            bridge = UdsBridge.create(uds);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            getLogger().severe("This UltimateDonutSmp version does not expose the economy/auction API this add-on needs: " + ex);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        webhook = new DiscordWebhook(this);

        EcoWatchCommand command = new EcoWatchCommand(this);
        PluginCommand pluginCommand = getCommand("ecowatch");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
        }

        startWatching();
    }

    @Override
    public void onDisable() {
        stopWatching();
    }

    public void reloadWatchdog() {
        reloadConfig();
        stopWatching();
        startWatching();
    }

    public DiscordWebhook getWebhook() {
        return webhook;
    }

    private void startWatching() {
        EcoWatchConfig config = loadConfigValues();
        webhook.configure(config.webhookUrl(), config.webhookUsername());

        payWatcher = new PayCommandWatcher(webhook, config);
        getServer().getPluginManager().registerEvents(payWatcher, this);

        long intervalTicks = Math.max(20L, config.pollIntervalSeconds() * 20L);
        balanceTask = getServer().getScheduler().runTaskTimer(this, new BalanceWatcher(this, bridge, webhook, config), intervalTicks, intervalTicks);
        auctionTask = getServer().getScheduler().runTaskTimer(this, new AuctionWatcher(this, bridge, webhook, config), intervalTicks, intervalTicks);

        getLogger().info("Watching UltimateDonutSmp's economy and auction house every " + config.pollIntervalSeconds() + "s. Webhook configured: " + webhook.isConfigured());
    }

    private void stopWatching() {
        if (balanceTask != null) {
            balanceTask.cancel();
            balanceTask = null;
        }
        if (auctionTask != null) {
            auctionTask.cancel();
            auctionTask = null;
        }
        if (payWatcher != null) {
            HandlerList.unregisterAll(payWatcher);
            payWatcher = null;
        }
    }

    private EcoWatchConfig loadConfigValues() {
        FileConfiguration c = getConfig();
        return new EcoWatchConfig(
                c.getString("discord.webhook-url", ""),
                c.getString("discord.username", "Economy Watchdog"),
                Math.max(5, c.getInt("poll-interval-seconds", 60)),
                c.getBoolean("balance-jump.enabled", true),
                c.getDouble("balance-jump.min-absolute-delta", 100000.0),
                c.getDouble("balance-jump.min-percent-delta", 300.0),
                c.getBoolean("large-transfer.enabled", true),
                c.getDouble("large-transfer.min-amount", 100000.0),
                c.getBoolean("auction.enabled", true),
                c.getDouble("auction.high-price-flag", 1000000.0),
                c.getDouble("auction.low-price-flag", 1.0),
                c.getBoolean("auction.repeat-trading.enabled", true),
                Math.max(1, c.getInt("auction.repeat-trading.window-minutes", 10)),
                Math.max(2, c.getInt("auction.repeat-trading.min-trades", 3))
        );
    }
}
