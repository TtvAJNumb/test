package com.donututils.stockmarket;

import com.donututils.stockmarket.command.PortfolioCommand;
import com.donututils.stockmarket.command.StockAdminCommand;
import com.donututils.stockmarket.command.StockCommand;
import com.donututils.stockmarket.command.StockLeaderboardCommand;
import com.donututils.stockmarket.config.StockMarketConfig;
import com.donututils.stockmarket.discord.DiscordWebhook;
import com.donututils.stockmarket.discord.StockAlerts;
import com.donututils.stockmarket.economy.VaultEconomyBridge;
import com.donututils.stockmarket.engine.PriceEngine;
import com.donututils.stockmarket.engine.StockRegistry;
import com.donututils.stockmarket.gui.MenuClickListener;
import com.donututils.stockmarket.gui.StockMenus;
import com.donututils.stockmarket.listener.ChatQuantityPrompt;
import com.donututils.stockmarket.service.DividendPool;
import com.donututils.stockmarket.service.DividendService;
import com.donututils.stockmarket.service.MarketAdminService;
import com.donututils.stockmarket.service.MarketSummaryTask;
import com.donututils.stockmarket.service.PortfolioService;
import com.donututils.stockmarket.service.TradingService;
import com.donututils.stockmarket.storage.HolderIndexStore;
import com.donututils.stockmarket.storage.PortfolioStore;
import com.donututils.stockmarket.storage.PriceHistoryStore;
import com.donututils.stockmarket.storage.StockStore;
import com.donututils.stockmarket.storage.TransactionLogStore;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class StockMarketPlugin extends JavaPlugin {

    private static final int MAX_VAULT_RETRIES = 10;

    private StockRegistry registry;
    private PriceHistoryStore historyStore;
    private PortfolioStore portfolioStore;
    private HolderIndexStore holderIndex;
    private TransactionLogStore transactionLog;

    private VaultEconomyBridge economy;
    private DiscordWebhook webhook;
    private StockAlerts alerts;

    private PriceEngine priceEngine;
    private TradingService tradingService;
    private PortfolioService portfolioService;
    private MarketAdminService adminService;
    private DividendService dividendService;
    private MarketSummaryTask summaryTask;
    private ChatQuantityPrompt quantityPrompt;

    private volatile StockMarketConfig config;

    private BukkitTask tickTask;
    private BukkitTask dividendTask;
    private BukkitTask summaryBukkitTask;
    private BukkitTask autosaveTask;
    private int vaultRetryAttempts;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault is not installed. StockMarket needs Vault plus any economy plugin that hooks into it. Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        tryResolveVaultAndFinishEnable();
    }

    private void tryResolveVaultAndFinishEnable() {
        try {
            economy = VaultEconomyBridge.create();
        } catch (ReflectiveOperationException | RuntimeException ex) {
            getLogger().severe("Vault's economy API doesn't look like what this plugin expects: " + ex);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (economy == null) {
            vaultRetryAttempts++;
            if (vaultRetryAttempts > MAX_VAULT_RETRIES) {
                getLogger().severe("No economy plugin registered with Vault after waiting. Install an economy plugin that hooks into Vault. Disabling.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            getServer().getScheduler().runTaskLater(this, this::tryResolveVaultAndFinishEnable, 20L);
            return;
        }

        finishEnable();
    }

    private void finishEnable() {
        if (!new java.io.File(getDataFolder(), "stocks.yml").exists()) {
            saveResource("stocks.yml", false);
        }
        registry = new StockRegistry(new StockStore(getDataFolder(), getLogger()));
        historyStore = new PriceHistoryStore(getDataFolder(), getLogger());
        portfolioStore = new PortfolioStore(getDataFolder(), getLogger());
        holderIndex = new HolderIndexStore(getDataFolder(), getLogger());
        transactionLog = new TransactionLogStore(getDataFolder(), getLogger());

        webhook = new DiscordWebhook(this);
        alerts = new StockAlerts(webhook, this::getStockMarketConfig);

        priceEngine = new PriceEngine(registry, historyStore, alerts, this::getStockMarketConfig);
        DividendPool dividendPool = new DividendPool();
        tradingService = new TradingService(registry, economy, portfolioStore, holderIndex, transactionLog, this::getStockMarketConfig, dividendPool);
        portfolioService = new PortfolioService(registry, portfolioStore, economy);
        adminService = new MarketAdminService(registry, portfolioStore, holderIndex, economy, alerts);
        dividendService = new DividendService(registry, holderIndex, portfolioStore, economy, dividendPool, getLogger());
        summaryTask = new MarketSummaryTask(registry, alerts, this::getStockMarketConfig);
        quantityPrompt = new ChatQuantityPrompt(this);

        StockMenus menus = new StockMenus(registry, tradingService, portfolioService, quantityPrompt, this::getStockMarketConfig);

        getServer().getPluginManager().registerEvents(new MenuClickListener(), this);
        getServer().getPluginManager().registerEvents(quantityPrompt, this);

        registerCommand("stocks", new StockCommand(registry, tradingService, menus));
        registerCommand("portfolio", new PortfolioCommand(menus));
        registerCommand("stockleaderboard", new StockLeaderboardCommand(portfolioStore, portfolioService));
        registerCommand("stockadmin", new StockAdminCommand(this, registry, adminService));

        startTasks();

        getLogger().info("StockMarket enabled - bridged to Vault economy provider, tracking " + registry.all().size() + " stock(s).");
    }

    @Override
    public void onDisable() {
        stopTasks();
        if (registry != null) {
            registry.saveAll();
        }
    }

    public void reloadStockMarket() {
        reloadConfig();
        config = loadConfigValues();
        webhook.configure(config.webhookUrl(), config.webhookUsername());
        stopTasks();
        startTasks();
    }

    private void startTasks() {
        config = loadConfigValues();
        webhook.configure(config.webhookUrl(), config.webhookUsername());

        long tickTicks = Math.max(20L, config.tickIntervalSeconds() * 20L);
        tickTask = getServer().getScheduler().runTaskTimer(this, priceEngine::tick, tickTicks, tickTicks);
        dividendTask = getServer().getScheduler().runTaskTimerAsynchronously(this, dividendService, 20L * 60L, 20L * 60L);
        summaryBukkitTask = getServer().getScheduler().runTaskTimerAsynchronously(this, summaryTask, 20L * 60L * 10L, 20L * 60L * 10L);
        autosaveTask = getServer().getScheduler().runTaskTimer(this, () -> registry.saveAll(), 20L * 60L * 5L, 20L * 60L * 5L);
    }

    private void stopTasks() {
        cancel(tickTask);
        cancel(dividendTask);
        cancel(summaryBukkitTask);
        cancel(autosaveTask);
        tickTask = null;
        dividendTask = null;
        summaryBukkitTask = null;
        autosaveTask = null;
    }

    private void cancel(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        } else {
            getLogger().warning("plugin.yml is missing the '" + name + "' command definition.");
        }
    }

    private StockMarketConfig getStockMarketConfig() {
        return config;
    }

    private StockMarketConfig loadConfigValues() {
        FileConfiguration c = getConfig();
        return new StockMarketConfig(
                Math.max(5, c.getInt("tick-interval-seconds", 30)),
                Math.max(1, c.getInt("trading-day-length-minutes", 60)),
                c.getDouble("max-trade-impact-percent-per-tick", 2.0),
                c.getDouble("circuit-breaker-threshold-percent", 15.0),
                Math.max(1, c.getInt("circuit-breaker-cooldown-seconds", 300)),
                c.getDouble("broker-fee-percent", 0.5),
                c.getDouble("max-shares-per-player-percent", 10.0),
                Math.max(0, c.getInt("trade-rate-limit-millis", 1000)),
                c.getDouble("big-mover-alert-threshold-percent", 5.0),
                c.getString("discord.webhook-url", ""),
                c.getString("discord.username", "StockMarket"),
                c.getBoolean("discord.alerts.big-mover", true),
                c.getBoolean("discord.alerts.circuit-breaker", true),
                c.getBoolean("discord.alerts.ipo", true),
                c.getBoolean("discord.alerts.split", true),
                c.getBoolean("discord.alerts.delisting", true),
                c.getBoolean("discord.daily-summary.enabled", false),
                c.getInt("discord.daily-summary.hour-of-day-utc", 12)
        );
    }
}
