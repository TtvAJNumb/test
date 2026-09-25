package com.donututils.purchasealert;

import com.donututils.purchasealert.command.PurchaseAlertCommand;
import com.donututils.purchasealert.config.AlertConfig;
import com.donututils.purchasealert.discord.DiscordWebhook;
import com.donututils.purchasealert.http.BackendOrdersClient;
import com.donututils.purchasealert.watch.PurchaseWatcher;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Reads the same backend StoreBridge already polls, using only its read-only order-listing
 * endpoint, and posts a Discord alert for each newly delivered purchase. Never touches
 * StoreBridge's files or classes - the two plugins don't know about each other at all, they just
 * happen to read the same backend.
 */
public final class PurchaseAlertPlugin extends JavaPlugin {

    private DiscordWebhook webhook;
    private BukkitTask task;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        webhook = new DiscordWebhook(this);

        PurchaseAlertCommand command = new PurchaseAlertCommand(this);
        PluginCommand pluginCommand = getCommand("purchasealert");
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
        AlertConfig config = loadConfigValues();
        webhook.configure(config.webhookUrl(), config.webhookUsername());

        if (config.backendUrl().isBlank() || config.pluginKey().isBlank()) {
            getLogger().warning("backend_url or plugin_key is not set - copy the same two values from StoreBridge's own config.yml. Purchase polling is paused until then.");
            return;
        }

        BackendOrdersClient client = new BackendOrdersClient(config.backendUrl(), config.pluginKey());
        PurchaseWatcher watcher = new PurchaseWatcher(this, client, webhook, config);

        long intervalTicks = Math.max(20L, config.pollIntervalSeconds() * 20L);
        task = getServer().getScheduler().runTaskTimerAsynchronously(this, watcher, intervalTicks, intervalTicks);

        getLogger().info("Watching for new store purchases every " + config.pollIntervalSeconds()
                + "s. Webhook configured: " + webhook.isConfigured());
    }

    private void stopWatching() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private AlertConfig loadConfigValues() {
        FileConfiguration c = getConfig();
        return new AlertConfig(
                c.getString("backend_url", ""),
                c.getString("plugin_key", ""),
                Math.max(5, c.getInt("poll_interval_seconds", 15)),
                Math.max(1, Math.min(25, c.getInt("order_limit", 10))),
                c.getString("discord.webhook-url", ""),
                c.getString("discord.username", "Store Purchases")
        );
    }
}
