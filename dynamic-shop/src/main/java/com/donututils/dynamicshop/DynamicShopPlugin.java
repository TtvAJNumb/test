package com.donututils.dynamicshop;

import com.donututils.dynamicshop.command.ShopAdminCommand;
import com.donututils.dynamicshop.command.ShopCommand;
import com.donututils.dynamicshop.config.DynamicShopConfig;
import com.donututils.dynamicshop.config.Messages;
import com.donututils.dynamicshop.currency.CurrencyRegistry;
import com.donututils.dynamicshop.currency.PlayerPointsCurrencyBridge;
import com.donututils.dynamicshop.currency.VaultCurrencyBridge;
import com.donututils.dynamicshop.engine.PlayerDataRegistry;
import com.donututils.dynamicshop.engine.PricingEngine;
import com.donututils.dynamicshop.engine.ShopItemRegistry;
import com.donututils.dynamicshop.gui.ShopMenuClickListener;
import com.donututils.dynamicshop.http.DynamicShopHttpServer;
import com.donututils.dynamicshop.listener.ChatQuantityPrompt;
import com.donututils.dynamicshop.listener.ItemCollectionListener;
import com.donututils.dynamicshop.service.EconomyStatsService;
import com.donututils.dynamicshop.service.LoanService;
import com.donututils.dynamicshop.service.ShopGuiService;
import com.donututils.dynamicshop.service.TradingService;
import com.donututils.dynamicshop.storage.LoanStore;
import com.donututils.dynamicshop.storage.PlayerDataStore;
import com.donututils.dynamicshop.storage.ShopItemStore;
import com.donututils.dynamicshop.storage.TransactionLogStore;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;

/**
 * A general-purpose, config-driven dynamic-pricing item shop: supply/demand-adjusted prices,
 * multi-currency support (Vault money + PlayerPoints shards), auto-sell, exploit protection, loans,
 * and basic economy stats. Every major feature can be switched off in config.yml without removing
 * the plugin. Standalone - doesn't depend on any other plugin in this repo.
 */
public final class DynamicShopPlugin extends JavaPlugin {

    private static final int MAX_CURRENCY_RETRIES = 10;

    private ShopItemRegistry itemRegistry;
    private PlayerDataRegistry playerDataRegistry;
    private CurrencyRegistry currencyRegistry;
    private TransactionLogStore transactionLog;
    private LoanStore loanStore;

    private PricingEngine pricingEngine;
    private TradingService tradingService;
    private ShopGuiService guiService;
    private LoanService loanService;
    private EconomyStatsService economyStatsService;
    private DynamicShopHttpServer httpServer;
    private ChatQuantityPrompt quantityPrompt;

    private volatile DynamicShopConfig config;
    private volatile Messages messages;

    private BukkitTask tickTask;
    private BukkitTask autosaveTask;
    private int currencyRetryAttempts;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "shop-items.yml").exists()) {
            saveResource("shop-items.yml", false);
        }
        if (!new File(getDataFolder(), "messages.yml").exists()) {
            saveResource("messages.yml", false);
        }

        config = loadConfigValues();
        messages = loadMessages();

        resolveCurrenciesAndFinishEnable();
    }

    private void resolveCurrenciesAndFinishEnable() {
        currencyRegistry = new CurrencyRegistry();
        boolean stillWaiting = false;

        if (config.moneyEnabled()) {
            try {
                VaultCurrencyBridge money = VaultCurrencyBridge.create();
                if (money != null) {
                    currencyRegistry.register(money);
                } else {
                    stillWaiting = true;
                }
            } catch (ReflectiveOperationException | RuntimeException ex) {
                getLogger().warning("Money currency enabled but Vault's economy API didn't look as expected: " + ex);
            }
        }
        if (config.shardsEnabled()) {
            try {
                PlayerPointsCurrencyBridge shards = PlayerPointsCurrencyBridge.create(config.shardsDisplayName());
                if (shards != null) {
                    currencyRegistry.register(shards);
                } else {
                    stillWaiting = true;
                }
            } catch (ReflectiveOperationException | RuntimeException ex) {
                getLogger().warning("Shards currency enabled but PlayerPoints' API didn't look as expected: " + ex);
            }
        }

        if (stillWaiting && currencyRegistry.isEmpty() && currencyRetryAttempts < MAX_CURRENCY_RETRIES) {
            currencyRetryAttempts++;
            getServer().getScheduler().runTaskLater(this, this::resolveCurrenciesAndFinishEnable, 20L);
            return;
        }

        if (currencyRegistry.isEmpty()) {
            getLogger().warning("No currency providers are available (checked Vault/PlayerPoints) - "
                    + "buying and selling will fail until one becomes available. Use /shopadmin reload once it is.");
        }

        finishEnable();
    }

    private void finishEnable() {
        itemRegistry = new ShopItemRegistry(new ShopItemStore(getDataFolder(), getLogger()));
        playerDataRegistry = new PlayerDataRegistry(new PlayerDataStore(getDataFolder(), getLogger()));
        transactionLog = new TransactionLogStore(getDataFolder(), getLogger());
        loanStore = new LoanStore(getDataFolder(), getLogger());
        quantityPrompt = new ChatQuantityPrompt(this);

        pricingEngine = new PricingEngine(itemRegistry);
        tradingService = new TradingService(itemRegistry, currencyRegistry, playerDataRegistry, transactionLog,
                this::getDynamicShopConfig, this::getMessages);
        guiService = new ShopGuiService(itemRegistry, currencyRegistry, tradingService, quantityPrompt, this::getDynamicShopConfig);
        loanService = new LoanService(loanStore, currencyRegistry, this::getDynamicShopConfig);
        economyStatsService = new EconomyStatsService(transactionLog, itemRegistry, loanService, this::getDynamicShopConfig);
        httpServer = new DynamicShopHttpServer(this, this::getDynamicShopConfig, itemRegistry, economyStatsService);

        getServer().getPluginManager().registerEvents(new ShopMenuClickListener(this::formatPrice), this);
        getServer().getPluginManager().registerEvents(quantityPrompt, this);
        getServer().getPluginManager().registerEvents(
                new ItemCollectionListener(itemRegistry, playerDataRegistry, tradingService, this::getDynamicShopConfig), this);

        registerCommand("shop", new ShopCommand(this, guiService, itemRegistry, playerDataRegistry, tradingService,
                loanService, economyStatsService, this::getDynamicShopConfig));
        registerCommand("shopadmin", new ShopAdminCommand(this, itemRegistry, currencyRegistry));

        startTasks();
        httpServer.start();

        getLogger().info("DynamicShop enabled with " + currencyRegistry.all().size() + " currenc"
                + (currencyRegistry.all().size() == 1 ? "y" : "ies") + " and " + itemRegistry.all().size() + " item(s).");
    }

    @Override
    public void onDisable() {
        stopTasks();
        if (httpServer != null) {
            httpServer.stop();
        }
        if (itemRegistry != null) {
            itemRegistry.saveAll();
        }
        if (playerDataRegistry != null) {
            playerDataRegistry.saveAll();
        }
        if (loanService != null) {
            loanService.saveAll();
        }
    }

    public void reloadDynamicShop() {
        reloadConfig();
        config = loadConfigValues();
        messages = loadMessages();
        stopTasks();
        if (httpServer != null) {
            httpServer.stop();
        }
        currencyRetryAttempts = 0;
        resolveCurrenciesAndFinishEnableForReload();
    }

    private void resolveCurrenciesAndFinishEnableForReload() {
        currencyRegistry = new CurrencyRegistry();
        if (config.moneyEnabled()) {
            try {
                VaultCurrencyBridge money = VaultCurrencyBridge.create();
                if (money != null) {
                    currencyRegistry.register(money);
                }
            } catch (ReflectiveOperationException | RuntimeException ex) {
                getLogger().warning("Money currency enabled but Vault's economy API didn't look as expected: " + ex);
            }
        }
        if (config.shardsEnabled()) {
            try {
                PlayerPointsCurrencyBridge shards = PlayerPointsCurrencyBridge.create(config.shardsDisplayName());
                if (shards != null) {
                    currencyRegistry.register(shards);
                }
            } catch (ReflectiveOperationException | RuntimeException ex) {
                getLogger().warning("Shards currency enabled but PlayerPoints' API didn't look as expected: " + ex);
            }
        }
        tradingService = new TradingService(itemRegistry, currencyRegistry, playerDataRegistry, transactionLog,
                this::getDynamicShopConfig, this::getMessages);
        guiService = new ShopGuiService(itemRegistry, currencyRegistry, tradingService, quantityPrompt, this::getDynamicShopConfig);
        loanService = new LoanService(loanStore, currencyRegistry, this::getDynamicShopConfig);
        economyStatsService = new EconomyStatsService(transactionLog, itemRegistry, loanService, this::getDynamicShopConfig);
        httpServer = new DynamicShopHttpServer(this, this::getDynamicShopConfig, itemRegistry, economyStatsService);
        startTasks();
        httpServer.start();
    }

    public void sendTutorial(Player player) {
        for (String line : config.tutorialLines()) {
            player.sendMessage(com.donututils.dynamicshop.gui.ShopPagedMenu.legacy(line));
        }
    }

    public DynamicShopConfig getDynamicShopConfig() {
        return config;
    }

    public Messages getMessages() {
        return messages;
    }

    private String formatPrice(String currencyId, double amount) {
        var provider = currencyRegistry.get(currencyId);
        return provider == null ? (amount + " " + currencyId) : provider.format(amount);
    }

    private void startTasks() {
        long tickTicks = Math.max(20L, config.tickIntervalSeconds() * 20L);
        long autosaveTicks = Math.max(20L, config.autosaveIntervalSeconds() * 20L);
        tickTask = getServer().getScheduler().runTaskTimer(this, pricingEngine::tick, tickTicks, tickTicks);
        autosaveTask = getServer().getScheduler().runTaskTimer(this, () -> {
            itemRegistry.saveAll();
            playerDataRegistry.saveAll();
            loanService.saveAll();
        }, autosaveTicks, autosaveTicks);
    }

    private void stopTasks() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
    }

    private DynamicShopConfig loadConfigValues() {
        FileConfiguration cfg = getConfig();
        return new DynamicShopConfig(
                cfg.getInt("tick-interval-seconds", 30),
                cfg.getInt("autosave-interval-seconds", 300),
                cfg.getBoolean("currencies.money.enabled", true),
                cfg.getString("currencies.money.display-name", "Money"),
                cfg.getBoolean("currencies.shards.enabled", true),
                cfg.getString("currencies.shards.display-name", "Shards"),
                cfg.getString("gui.title", "&8Server Shop"),
                cfg.getBoolean("features.auto-sell", true),
                cfg.getBoolean("features.loans", true),
                cfg.getBoolean("features.web-server", false),
                cfg.getBoolean("features.gdp-stats", true),
                cfg.getBoolean("features.tutorial", true),
                cfg.getBoolean("features.purchase-restriction", false),
                cfg.getInt("exploit-protection.transaction-cooldown-seconds", 1),
                cfg.getInt("exploit-protection.global-max-quantity-per-transaction", 6400),
                cfg.getDouble("loans.max-loan-amount", 10000.0),
                cfg.getDouble("loans.interest-rate-percent-per-day", 5.0),
                cfg.getInt("loans.max-outstanding-loans-per-player", 1),
                cfg.getString("web-server.bind-address", "127.0.0.1"),
                cfg.getInt("web-server.port", 8082),
                cfg.getString("web-server.api-key", ""),
                cfg.getInt("gdp.window-hours", 24),
                cfg.getStringList("tutorial.lines")
        );
    }

    private Messages loadMessages() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));
        return new Messages(yaml);
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        } else {
            getLogger().warning("plugin.yml is missing the '" + name + "' command definition.");
        }
    }
}
