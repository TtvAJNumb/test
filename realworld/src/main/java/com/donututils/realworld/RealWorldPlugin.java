package com.donututils.realworld;

import com.donututils.realworld.aichat.client.ChatClient;
import com.donututils.realworld.aichat.client.ClaudeApiClient;
import com.donututils.realworld.aichat.client.GeminiApiClient;
import com.donututils.realworld.aichat.client.OllamaApiClient;
import com.donututils.realworld.aichat.command.AICommand;
import com.donututils.realworld.aichat.config.AIChatConfig;
import com.donututils.realworld.aichat.memory.ConversationMemory;
import com.donututils.realworld.aichat.tool.ServerContextService;

import com.donututils.realworld.arsenal.command.ArsenalCommand;
import com.donututils.realworld.arsenal.config.ArsenalConfig;
import com.donututils.realworld.arsenal.config.WeaponDefinition;
import com.donututils.realworld.arsenal.weapon.FireListener;
import com.donututils.realworld.arsenal.weapon.ReloadListener;
import com.donututils.realworld.arsenal.weapon.WeaponItemFactory;
import com.donututils.realworld.arsenal.weapon.WeaponKeys;
import com.donututils.realworld.arsenal.weapon.WeaponManager;

import com.donututils.realworld.careers.citizen.CitizenManager;
import com.donututils.realworld.careers.command.CareerCommand;
import com.donututils.realworld.careers.config.AgeTier;
import com.donututils.realworld.careers.config.CareersConfig;
import com.donututils.realworld.careers.config.JobDefinition;
import com.donututils.realworld.careers.job.TaskBonusListener;
import com.donututils.realworld.careers.onboarding.OnboardingGuiService;
import com.donututils.realworld.careers.onboarding.OnboardingListener;

import com.donututils.realworld.dynamicshop.command.ShopAdminCommand;
import com.donututils.realworld.dynamicshop.command.ShopCommand;
import com.donututils.realworld.dynamicshop.config.DynamicShopConfig;
import com.donututils.realworld.dynamicshop.config.Messages;
import com.donututils.realworld.dynamicshop.currency.CurrencyRegistry;
import com.donututils.realworld.dynamicshop.currency.PlayerPointsCurrencyBridge;
import com.donututils.realworld.dynamicshop.currency.VaultCurrencyBridge;
import com.donututils.realworld.dynamicshop.engine.PlayerDataRegistry;
import com.donututils.realworld.dynamicshop.engine.PricingEngine;
import com.donututils.realworld.dynamicshop.engine.ShopItemRegistry;
import com.donututils.realworld.dynamicshop.gui.ShopMenuClickListener;
import com.donututils.realworld.dynamicshop.http.DynamicShopHttpServer;
import com.donututils.realworld.dynamicshop.listener.ItemCollectionListener;
import com.donututils.realworld.dynamicshop.model.PlayerShopData;
import com.donututils.realworld.dynamicshop.service.EconomyStatsService;
import com.donututils.realworld.dynamicshop.service.LoanService;
import com.donututils.realworld.dynamicshop.service.ShopGuiService;
import com.donututils.realworld.dynamicshop.storage.LoanStore;
import com.donututils.realworld.dynamicshop.storage.PlayerDataStore;
import com.donututils.realworld.dynamicshop.storage.ShopItemStore;

import com.donututils.realworld.ledger.bank.BankManager;
import com.donututils.realworld.ledger.command.BankCommand;
import com.donututils.realworld.ledger.command.CorpCommand;
import com.donututils.realworld.ledger.command.LedgerAdminCommand;
import com.donututils.realworld.ledger.command.LoanCommand;
import com.donututils.realworld.ledger.config.LedgerConfig;
import com.donututils.realworld.ledger.config.LoanTier;
import com.donututils.realworld.ledger.corp.CorporationManager;
import com.donututils.realworld.ledger.credit.CreditScoreManager;
import com.donututils.realworld.ledger.economy.LedgerEconomyProvider;
import com.donututils.realworld.ledger.loan.LoanManager;
import com.donututils.realworld.ledger.stock.ShareTradingManager;
import com.donututils.realworld.ledger.tax.TaxManager;

import com.donututils.realworld.municipal.claim.ClaimManager;
import com.donututils.realworld.municipal.claim.ClaimProtectionListener;
import com.donututils.realworld.municipal.command.ClaimCommand;
import com.donututils.realworld.municipal.command.CourtCommand;
import com.donututils.realworld.municipal.command.LocationCommand;
import com.donututils.realworld.municipal.command.PermitCommand;
import com.donututils.realworld.municipal.command.PoliceCommand;
import com.donututils.realworld.municipal.config.MunicipalConfig;
import com.donututils.realworld.municipal.config.PermitDefinition;
import com.donututils.realworld.municipal.court.CourtManager;
import com.donututils.realworld.municipal.jail.JailListener;
import com.donututils.realworld.municipal.jail.JailManager;
import com.donututils.realworld.municipal.location.LocationManager;
import com.donututils.realworld.municipal.permit.PermitEnforcementListener;
import com.donututils.realworld.municipal.permit.PermitManager;

import com.donututils.realworld.motors.command.MotorsCommand;
import com.donututils.realworld.motors.config.MotorsConfig;
import com.donututils.realworld.motors.config.VehicleDefinition;
import com.donututils.realworld.motors.vehicle.PlaceListener;
import com.donututils.realworld.motors.vehicle.VehicleItemFactory;
import com.donututils.realworld.motors.vehicle.VehicleKeys;
import com.donututils.realworld.motors.vehicle.VehicleManager;

import com.donututils.realworld.stockmarket.command.PortfolioCommand;
import com.donututils.realworld.stockmarket.command.StockAdminCommand;
import com.donututils.realworld.stockmarket.command.StockLeaderboardCommand;
import com.donututils.realworld.stockmarket.config.StockMarketConfig;
import com.donututils.realworld.stockmarket.discord.DiscordWebhook;
import com.donututils.realworld.stockmarket.discord.StockAlerts;
import com.donututils.realworld.stockmarket.economy.VaultEconomyBridge;
import com.donututils.realworld.stockmarket.engine.PriceEngine;
import com.donututils.realworld.stockmarket.engine.StockRegistry;
import com.donututils.realworld.stockmarket.gui.MenuClickListener;
import com.donututils.realworld.stockmarket.gui.StockMenus;
import com.donututils.realworld.stockmarket.listener.ChatQuantityPrompt;
import com.donututils.realworld.stockmarket.service.DividendPool;
import com.donututils.realworld.stockmarket.service.DividendService;
import com.donututils.realworld.stockmarket.service.MarketAdminService;
import com.donututils.realworld.stockmarket.service.MarketSummaryTask;
import com.donututils.realworld.stockmarket.service.PortfolioService;
import com.donututils.realworld.stockmarket.service.TradingService;
import com.donututils.realworld.stockmarket.storage.HolderIndexStore;
import com.donututils.realworld.stockmarket.storage.PortfolioStore;
import com.donututils.realworld.stockmarket.storage.PriceHistoryStore;
import com.donututils.realworld.stockmarket.storage.StockStore;
import com.donututils.realworld.stockmarket.storage.TransactionLogStore;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * RealWorld: every economy/society subsystem built this session, combined into one plugin -
 * Ledger (banking/credit/loans/corporations, and the server's actual Vault economy provider),
 * StockMarket, DynamicShop, AIChat, Municipal (courts/jail/permits/land claims), Arsenal (firearms),
 * Motors (vehicles), and Careers (age/job/wages). Each subsystem's manager/service/command classes are
 * unchanged from their standalone-plugin form - this class only replaces the 8 separate onEnable()
 * wirings with one, in dependency order (Ledger's economy must register with Vault before anything
 * else that spends or earns money is constructed).
 */
public final class RealWorldPlugin extends JavaPlugin {

    // ── Ledger ───────────────────────────────────────────────────────────────
    private com.donututils.realworld.ledger.db.DatabaseManager ledgerDatabase;
    private LedgerEconomyProvider economyProvider;
    private BankManager bankManager;
    private CreditScoreManager creditScoreManager;
    private LoanManager loanManager;
    private CorporationManager corporationManager;
    private ShareTradingManager shareTradingManager;
    private TaxManager taxManager;
    private volatile LedgerConfig ledgerConfig;
    private final List<BukkitTask> ledgerTasks = new ArrayList<>();

    // ── StockMarket ──────────────────────────────────────────────────────────
    private StockRegistry stockRegistry;
    private PriceHistoryStore stockHistoryStore;
    private PortfolioStore stockPortfolioStore;
    private HolderIndexStore stockHolderIndex;
    private TransactionLogStore stockTransactionLog;
    private VaultEconomyBridge stockEconomy;
    private DiscordWebhook stockWebhook;
    private StockAlerts stockAlerts;
    private PriceEngine priceEngine;
    private TradingService stockTradingService;
    private PortfolioService stockPortfolioService;
    private MarketAdminService stockAdminService;
    private DividendService stockDividendService;
    private MarketSummaryTask stockSummaryTask;
    private ChatQuantityPrompt stockQuantityPrompt;
    private volatile StockMarketConfig stockMarketConfig;
    private BukkitTask stockTickTask;
    private BukkitTask stockDividendTask;
    private BukkitTask stockSummaryBukkitTask;
    private BukkitTask stockAutosaveTask;

    // ── DynamicShop ──────────────────────────────────────────────────────────
    private static final int MAX_CURRENCY_RETRIES = 10;
    private ShopItemRegistry shopItemRegistry;
    private PlayerDataRegistry shopPlayerDataRegistry;
    private CurrencyRegistry shopCurrencyRegistry;
    private com.donututils.realworld.dynamicshop.storage.TransactionLogStore shopTransactionLog;
    private LoanStore shopLoanStore;
    private PricingEngine shopPricingEngine;
    private com.donututils.realworld.dynamicshop.service.TradingService shopTradingService;
    private ShopGuiService shopGuiService;
    private LoanService shopLoanService;
    private EconomyStatsService shopEconomyStatsService;
    private DynamicShopHttpServer shopHttpServer;
    private com.donututils.realworld.dynamicshop.listener.ChatQuantityPrompt shopQuantityPrompt;
    private volatile DynamicShopConfig dynamicShopConfig;
    private volatile Messages shopMessages;
    private BukkitTask shopTickTask;
    private BukkitTask shopAutosaveTask;
    private int shopCurrencyRetryAttempts;

    // ── Municipal ────────────────────────────────────────────────────────────
    private com.donututils.realworld.municipal.db.DatabaseManager municipalDatabase;
    private com.donututils.realworld.municipal.economy.VaultEconomyBridge municipalEconomy;
    private PermitManager permitManager;
    private CourtManager courtManager;
    private JailManager jailManager;
    private ClaimManager claimManager;
    private LocationManager locationManager;
    private volatile MunicipalConfig municipalConfig;
    private BukkitTask municipalReleaseTask;
    private BukkitTask municipalPropertyTaxTask;

    // ── Arsenal ──────────────────────────────────────────────────────────────
    private WeaponKeys arsenalKeys;
    private WeaponItemFactory arsenalItemFactory;
    private WeaponManager arsenalWeaponManager;
    private volatile ArsenalConfig arsenalConfig;

    // ── Motors ───────────────────────────────────────────────────────────────
    private com.donututils.realworld.motors.db.DatabaseManager motorsDatabase;
    private com.donututils.realworld.motors.economy.VaultEconomyBridge motorsEconomy;
    private VehicleManager motorsVehicleManager;
    private volatile MotorsConfig motorsConfig;
    private BukkitTask motorsTickTask;

    // ── Careers ──────────────────────────────────────────────────────────────
    private com.donututils.realworld.careers.db.DatabaseManager careersDatabase;
    private com.donututils.realworld.careers.economy.VaultEconomyBridge careersEconomy;
    private CitizenManager citizenManager;
    private volatile CareersConfig careersConfig;
    private BukkitTask careersWageTask;

    // ── AIChat ───────────────────────────────────────────────────────────────
    private ChatClient aiChatClient;
    private ConversationMemory aiMemory;
    private volatile ServerContextService aiServerContext;
    private volatile AIChatConfig aiChatConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "stocks.yml").exists()) {
            saveResource("stocks.yml", false);
        }
        if (!new File(getDataFolder(), "shop-items.yml").exists()) {
            saveResource("shop-items.yml", false);
        }
        if (!new File(getDataFolder(), "messages.yml").exists()) {
            saveResource("messages.yml", false);
        }

        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault is not installed. RealWorld needs Vault (Ledger registers itself "
                    + "as the economy provider, so no separate economy plugin is needed). Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        setupLedger();
        if (!isEnabled()) {
            return;
        }
        setupStockMarket();
        setupDynamicShop();
        setupMunicipal();
        setupArsenal();
        setupMotors();
        setupCareers();
        setupAIChat();

        getLogger().info("RealWorld enabled - Ledger, StockMarket, DynamicShop, AIChat, Municipal, Arsenal, Motors, and Careers are all live.");
    }

    @Override
    public void onDisable() {
        for (BukkitTask task : ledgerTasks) {
            task.cancel();
        }
        ledgerTasks.clear();
        getServer().getServicesManager().unregisterAll(this);
        if (ledgerDatabase != null) {
            ledgerDatabase.shutdown();
        }

        cancel(stockTickTask);
        cancel(stockDividendTask);
        cancel(stockSummaryBukkitTask);
        cancel(stockAutosaveTask);
        if (stockRegistry != null) {
            stockRegistry.saveAll();
        }

        cancel(shopTickTask);
        cancel(shopAutosaveTask);
        if (shopHttpServer != null) {
            shopHttpServer.stop();
        }
        if (shopItemRegistry != null) {
            shopItemRegistry.saveAll();
        }
        if (shopPlayerDataRegistry != null) {
            shopPlayerDataRegistry.saveAll();
        }
        if (shopLoanService != null) {
            shopLoanService.saveAll();
        }

        cancel(municipalReleaseTask);
        cancel(municipalPropertyTaxTask);
        if (municipalDatabase != null) {
            municipalDatabase.shutdown();
        }

        cancel(motorsTickTask);
        if (motorsVehicleManager != null) {
            motorsVehicleManager.shutdownFlush();
        }
        if (motorsDatabase != null) {
            motorsDatabase.shutdown();
        }

        cancel(careersWageTask);
        if (careersDatabase != null) {
            careersDatabase.shutdown();
        }
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

    // ═══════════════════════════════════════════════════════════════════════
    // LEDGER
    // ═══════════════════════════════════════════════════════════════════════

    private void setupLedger() {
        ledgerConfig = loadLedgerConfig();

        ledgerDatabase = new com.donututils.realworld.ledger.db.DatabaseManager(getDataFolder(), getLogger());
        economyProvider = new LedgerEconomyProvider(this, ledgerDatabase, ledgerConfig.startingBalance());
        creditScoreManager = new CreditScoreManager(this, ledgerDatabase, this::getLedgerConfig);
        bankManager = new BankManager(this, ledgerDatabase, economyProvider, this::getLedgerConfig);
        loanManager = new LoanManager(this, ledgerDatabase, economyProvider, creditScoreManager, this::getLedgerConfig);
        corporationManager = new CorporationManager(this, ledgerDatabase, economyProvider, this::getLedgerConfig);
        shareTradingManager = new ShareTradingManager(this, ledgerDatabase, economyProvider, corporationManager, this::getLedgerConfig);
        taxManager = new TaxManager(this, economyProvider, bankManager, corporationManager, this::getLedgerConfig);

        getServer().getServicesManager().register(Economy.class, economyProvider, this, ServicePriority.Highest);
        getServer().getPluginManager().registerEvents(new LedgerJoinListener(), this);

        registerCommand("bank", new BankCommand(this, economyProvider, bankManager, creditScoreManager));
        registerCommand("loan", new LoanCommand(this, loanManager));
        registerCommand("corp", new CorpCommand(this, corporationManager, shareTradingManager));
        registerCommand("stock", new com.donututils.realworld.ledger.command.StockCommand(this, corporationManager, shareTradingManager, taxManager, economyProvider));
        registerCommand("ledgeradmin", new LedgerAdminCommand(this, loanManager));

        startLedgerTasks();
    }

    private final class LedgerJoinListener implements Listener {
        @EventHandler
        public void onJoin(PlayerJoinEvent event) {
            economyProvider.ensureAccount(event.getPlayer().getUniqueId());
        }
    }

    public void reloadLedger() {
        reloadConfig();
        ledgerConfig = loadLedgerConfig();
        for (BukkitTask task : ledgerTasks) {
            task.cancel();
        }
        ledgerTasks.clear();
        startLedgerTasks();
    }

    public LedgerConfig getLedgerConfig() {
        return ledgerConfig;
    }

    public com.donututils.realworld.ledger.db.DatabaseManager getDatabaseManager() {
        return ledgerDatabase;
    }

    public LedgerEconomyProvider getEconomyProvider() {
        return economyProvider;
    }

    private void startLedgerTasks() {
        long tickMinutesToTicks = 20L * 60L;
        ledgerTasks.add(getServer().getScheduler().runTaskTimerAsynchronously(this, bankManager::tickInterest,
                ledgerConfig.interestTickMinutes() * tickMinutesToTicks, ledgerConfig.interestTickMinutes() * tickMinutesToTicks));
        ledgerTasks.add(getServer().getScheduler().runTaskTimerAsynchronously(this, loanManager::tickLoans,
                ledgerConfig.loanPaymentTickMinutes() * tickMinutesToTicks, ledgerConfig.loanPaymentTickMinutes() * tickMinutesToTicks));
        ledgerTasks.add(getServer().getScheduler().runTaskTimerAsynchronously(this, shareTradingManager::tickPrices,
                ledgerConfig.corpPriceTickMinutes() * tickMinutesToTicks, ledgerConfig.corpPriceTickMinutes() * tickMinutesToTicks));

        long tickHoursToTicks = 20L * 60L * 60L;
        ledgerTasks.add(getServer().getScheduler().runTaskTimerAsynchronously(this, taxManager::collectWealthTax,
                ledgerConfig.wealthTaxTickHours() * tickHoursToTicks, ledgerConfig.wealthTaxTickHours() * tickHoursToTicks));
        ledgerTasks.add(getServer().getScheduler().runTaskTimerAsynchronously(this, taxManager::collectCorporateTax,
                ledgerConfig.corporateTaxTickHours() * tickHoursToTicks, ledgerConfig.corporateTaxTickHours() * tickHoursToTicks));
    }

    private LedgerConfig loadLedgerConfig() {
        FileConfiguration cfg = getConfig();

        List<LoanTier> tiers = new ArrayList<>();
        for (Map<?, ?> raw : cfg.getMapList("ledger.loans.tiers")) {
            int minScore = raw.get("min-score") instanceof Number n ? n.intValue() : 300;
            double maxLoanAmount = raw.get("max-loan-amount") instanceof Number n ? n.doubleValue() : 1000.0;
            double aprPercent = raw.get("apr-percent") instanceof Number n ? n.doubleValue() : 25.0;
            tiers.add(new LoanTier(minScore, maxLoanAmount, aprPercent));
        }
        if (tiers.isEmpty()) {
            tiers.add(new LoanTier(300, 1000.0, 25.0));
        }
        tiers.sort(Comparator.comparingInt(LoanTier::minScore).reversed());

        return new LedgerConfig(
                cfg.getDouble("ledger.starting-balance", 1000.0),
                cfg.getDouble("ledger.bank.savings-apy-percent", 4.0),
                cfg.getInt("ledger.bank.interest-tick-minutes", 15),
                cfg.getInt("ledger.credit.starting-score", 650),
                cfg.getInt("ledger.credit.points-per-on-time-payment", 5),
                cfg.getInt("ledger.credit.points-per-missed-payment", -25),
                cfg.getInt("ledger.credit.points-per-default", -100),
                tiers,
                cfg.getInt("ledger.loans.term-days", 30),
                cfg.getInt("ledger.loans.missed-payments-before-default", 3),
                cfg.getInt("ledger.loans.payment-tick-minutes", 15),
                cfg.getDouble("ledger.corporations.founding-cost", 2500.0),
                cfg.getInt("ledger.corporations.default-total-shares", 1000),
                cfg.getDouble("ledger.corporations.base-drift", 0.0001),
                cfg.getDouble("ledger.corporations.base-volatility", 0.01),
                cfg.getDouble("ledger.corporations.profit-price-sensitivity", 0.5),
                cfg.getInt("ledger.corporations.price-tick-minutes", 5),
                cfg.getInt("ledger.corporations.report-cooldown-minutes", 60),
                cfg.getDouble("ledger.corporations.max-report-impact-percent", 25.0),
                cfg.getDouble("ledger.tax.wealth-tax-percent", 1.0),
                cfg.getInt("ledger.tax.wealth-tax-tick-hours", 24),
                cfg.getDouble("ledger.tax.transaction-tax-percent", 2.0),
                cfg.getDouble("ledger.tax.corporate-tax-percent", 5.0),
                cfg.getInt("ledger.tax.corporate-tax-tick-hours", 24),
                cfg.getBoolean("ledger.tax.expense-writeoff-enabled", true),
                cfg.getDouble("ledger.tax.trust-shield-percent", 40.0),
                cfg.getDouble("ledger.tax.trust-setup-cost", 10000.0)
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STOCKMARKET
    // ═══════════════════════════════════════════════════════════════════════

    private void setupStockMarket() {
        try {
            stockEconomy = VaultEconomyBridge.create();
        } catch (ReflectiveOperationException | RuntimeException ex) {
            getLogger().severe("Vault's economy API doesn't look like what StockMarket expects: " + ex);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (stockEconomy == null) {
            getLogger().severe("Ledger didn't register a Vault economy - StockMarket can't function. Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        stockRegistry = new StockRegistry(new StockStore(getDataFolder(), getLogger()));
        stockHistoryStore = new PriceHistoryStore(getDataFolder(), getLogger());
        stockPortfolioStore = new PortfolioStore(getDataFolder(), getLogger());
        stockHolderIndex = new HolderIndexStore(getDataFolder(), getLogger());
        stockTransactionLog = new TransactionLogStore(getDataFolder(), getLogger());

        stockWebhook = new DiscordWebhook(this);
        stockAlerts = new StockAlerts(stockWebhook, this::getStockMarketConfig);

        priceEngine = new PriceEngine(stockRegistry, stockHistoryStore, stockAlerts, this::getStockMarketConfig);
        DividendPool dividendPool = new DividendPool();
        stockTradingService = new TradingService(stockRegistry, stockEconomy, stockPortfolioStore, stockHolderIndex, stockTransactionLog, this::getStockMarketConfig, dividendPool);
        stockPortfolioService = new PortfolioService(stockRegistry, stockPortfolioStore, stockEconomy);
        stockAdminService = new MarketAdminService(stockRegistry, stockPortfolioStore, stockHolderIndex, stockEconomy, stockAlerts);
        stockDividendService = new DividendService(stockRegistry, stockHolderIndex, stockPortfolioStore, stockEconomy, dividendPool, getLogger());
        stockSummaryTask = new MarketSummaryTask(stockRegistry, stockAlerts, this::getStockMarketConfig);
        stockQuantityPrompt = new ChatQuantityPrompt(this);

        StockMenus menus = new StockMenus(stockRegistry, stockTradingService, stockPortfolioService, stockQuantityPrompt, this::getStockMarketConfig);

        getServer().getPluginManager().registerEvents(new MenuClickListener(), this);
        getServer().getPluginManager().registerEvents(stockQuantityPrompt, this);

        registerCommand("stocks", new com.donututils.realworld.stockmarket.command.StockCommand(stockRegistry, stockTradingService, menus));
        registerCommand("portfolio", new PortfolioCommand(menus));
        registerCommand("stockleaderboard", new StockLeaderboardCommand(stockPortfolioStore, stockPortfolioService));
        registerCommand("stockadmin", new StockAdminCommand(this, stockRegistry, stockAdminService));

        startStockMarketTasks();
    }

    public void reloadStockMarket() {
        reloadConfig();
        stockMarketConfig = loadStockMarketConfig();
        stockWebhook.configure(stockMarketConfig.webhookUrl(), stockMarketConfig.webhookUsername());
        stopStockMarketTasks();
        startStockMarketTasks();
    }

    private void startStockMarketTasks() {
        stockMarketConfig = loadStockMarketConfig();
        stockWebhook.configure(stockMarketConfig.webhookUrl(), stockMarketConfig.webhookUsername());

        long tickTicks = Math.max(20L, stockMarketConfig.tickIntervalSeconds() * 20L);
        stockTickTask = getServer().getScheduler().runTaskTimer(this, priceEngine::tick, tickTicks, tickTicks);
        stockDividendTask = getServer().getScheduler().runTaskTimerAsynchronously(this, stockDividendService, 20L * 60L, 20L * 60L);
        stockSummaryBukkitTask = getServer().getScheduler().runTaskTimerAsynchronously(this, stockSummaryTask, 20L * 60L * 10L, 20L * 60L * 10L);
        stockAutosaveTask = getServer().getScheduler().runTaskTimer(this, () -> stockRegistry.saveAll(), 20L * 60L * 5L, 20L * 60L * 5L);
    }

    private void stopStockMarketTasks() {
        cancel(stockTickTask);
        cancel(stockDividendTask);
        cancel(stockSummaryBukkitTask);
        cancel(stockAutosaveTask);
        stockTickTask = null;
        stockDividendTask = null;
        stockSummaryBukkitTask = null;
        stockAutosaveTask = null;
    }

    private StockMarketConfig getStockMarketConfig() {
        return stockMarketConfig;
    }

    private StockMarketConfig loadStockMarketConfig() {
        FileConfiguration c = getConfig();
        return new StockMarketConfig(
                Math.max(5, c.getInt("stockmarket.tick-interval-seconds", 30)),
                Math.max(1, c.getInt("stockmarket.trading-day-length-minutes", 60)),
                c.getDouble("stockmarket.max-trade-impact-percent-per-tick", 2.0),
                c.getDouble("stockmarket.circuit-breaker-threshold-percent", 15.0),
                Math.max(1, c.getInt("stockmarket.circuit-breaker-cooldown-seconds", 300)),
                c.getDouble("stockmarket.broker-fee-percent", 0.5),
                c.getDouble("stockmarket.max-shares-per-player-percent", 10.0),
                Math.max(0, c.getInt("stockmarket.trade-rate-limit-millis", 1000)),
                c.getDouble("stockmarket.big-mover-alert-threshold-percent", 5.0),
                c.getString("stockmarket.discord.webhook-url", ""),
                c.getString("stockmarket.discord.username", "StockMarket"),
                c.getBoolean("stockmarket.discord.alerts.big-mover", true),
                c.getBoolean("stockmarket.discord.alerts.circuit-breaker", true),
                c.getBoolean("stockmarket.discord.alerts.ipo", true),
                c.getBoolean("stockmarket.discord.alerts.split", true),
                c.getBoolean("stockmarket.discord.alerts.delisting", true),
                c.getBoolean("stockmarket.discord.daily-summary.enabled", false),
                c.getInt("stockmarket.discord.daily-summary.hour-of-day-utc", 12)
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DYNAMICSHOP
    // ═══════════════════════════════════════════════════════════════════════

    private void setupDynamicShop() {
        dynamicShopConfig = loadDynamicShopConfig();
        shopMessages = loadShopMessages();
        resolveShopCurrenciesAndFinishEnable();
    }

    private void resolveShopCurrenciesAndFinishEnable() {
        shopCurrencyRegistry = new CurrencyRegistry();
        boolean stillWaiting = false;

        if (dynamicShopConfig.moneyEnabled()) {
            try {
                VaultCurrencyBridge money = VaultCurrencyBridge.create();
                if (money != null) {
                    shopCurrencyRegistry.register(money);
                } else {
                    stillWaiting = true;
                }
            } catch (ReflectiveOperationException | RuntimeException ex) {
                getLogger().warning("Money currency enabled but Vault's economy API didn't look as expected: " + ex);
            }
        }
        if (dynamicShopConfig.shardsEnabled()) {
            try {
                PlayerPointsCurrencyBridge shards = PlayerPointsCurrencyBridge.create(dynamicShopConfig.shardsDisplayName());
                if (shards != null) {
                    shopCurrencyRegistry.register(shards);
                } else {
                    stillWaiting = true;
                }
            } catch (ReflectiveOperationException | RuntimeException ex) {
                getLogger().warning("Shards currency enabled but PlayerPoints' API didn't look as expected: " + ex);
            }
        }

        if (stillWaiting && shopCurrencyRegistry.isEmpty() && shopCurrencyRetryAttempts < MAX_CURRENCY_RETRIES) {
            shopCurrencyRetryAttempts++;
            getServer().getScheduler().runTaskLater(this, this::resolveShopCurrenciesAndFinishEnable, 20L);
            return;
        }

        if (shopCurrencyRegistry.isEmpty()) {
            getLogger().warning("DynamicShop has no currency providers available (checked Vault/PlayerPoints) - "
                    + "buying and selling will fail until one becomes available. Use /shopadmin reload once it is.");
        }

        finishDynamicShopEnable();
    }

    private void finishDynamicShopEnable() {
        shopItemRegistry = new ShopItemRegistry(new ShopItemStore(getDataFolder(), getLogger()));
        shopPlayerDataRegistry = new PlayerDataRegistry(new PlayerDataStore(getDataFolder(), getLogger()));
        shopTransactionLog = new com.donututils.realworld.dynamicshop.storage.TransactionLogStore(getDataFolder(), getLogger());
        shopLoanStore = new LoanStore(getDataFolder(), getLogger());
        shopQuantityPrompt = new com.donututils.realworld.dynamicshop.listener.ChatQuantityPrompt(this);

        shopPricingEngine = new PricingEngine(shopItemRegistry);
        shopTradingService = new com.donututils.realworld.dynamicshop.service.TradingService(shopItemRegistry, shopCurrencyRegistry, shopPlayerDataRegistry, shopTransactionLog,
                this::getDynamicShopConfig, this::getMessages);
        shopGuiService = new ShopGuiService(shopItemRegistry, shopCurrencyRegistry, shopTradingService, shopQuantityPrompt, this::getDynamicShopConfig);
        shopLoanService = new LoanService(shopLoanStore, shopCurrencyRegistry, this::getDynamicShopConfig);
        shopEconomyStatsService = new EconomyStatsService(shopTransactionLog, shopItemRegistry, shopLoanService, this::getDynamicShopConfig);
        shopHttpServer = new DynamicShopHttpServer(this, this::getDynamicShopConfig, shopItemRegistry, shopEconomyStatsService);

        getServer().getPluginManager().registerEvents(new ShopMenuClickListener(this::formatShopPrice), this);
        getServer().getPluginManager().registerEvents(shopQuantityPrompt, this);
        getServer().getPluginManager().registerEvents(
                new ItemCollectionListener(shopItemRegistry, shopPlayerDataRegistry, shopTradingService, this::getDynamicShopConfig), this);

        registerCommand("shop", new ShopCommand(this));
        registerCommand("shopadmin", new ShopAdminCommand(this));

        startDynamicShopTasks();
        shopHttpServer.start();
    }

    public void reloadDynamicShop() {
        reloadConfig();
        dynamicShopConfig = loadDynamicShopConfig();
        shopMessages = loadShopMessages();
        stopDynamicShopTasks();
        if (shopHttpServer != null) {
            shopHttpServer.stop();
        }
        shopCurrencyRetryAttempts = 0;
        resolveShopCurrenciesAndFinishEnableForReload();
    }

    private void resolveShopCurrenciesAndFinishEnableForReload() {
        shopCurrencyRegistry = new CurrencyRegistry();
        if (dynamicShopConfig.moneyEnabled()) {
            try {
                VaultCurrencyBridge money = VaultCurrencyBridge.create();
                if (money != null) {
                    shopCurrencyRegistry.register(money);
                }
            } catch (ReflectiveOperationException | RuntimeException ex) {
                getLogger().warning("Money currency enabled but Vault's economy API didn't look as expected: " + ex);
            }
        }
        if (dynamicShopConfig.shardsEnabled()) {
            try {
                PlayerPointsCurrencyBridge shards = PlayerPointsCurrencyBridge.create(dynamicShopConfig.shardsDisplayName());
                if (shards != null) {
                    shopCurrencyRegistry.register(shards);
                }
            } catch (ReflectiveOperationException | RuntimeException ex) {
                getLogger().warning("Shards currency enabled but PlayerPoints' API didn't look as expected: " + ex);
            }
        }
        shopTradingService = new com.donututils.realworld.dynamicshop.service.TradingService(shopItemRegistry, shopCurrencyRegistry, shopPlayerDataRegistry, shopTransactionLog,
                this::getDynamicShopConfig, this::getMessages);
        shopGuiService = new ShopGuiService(shopItemRegistry, shopCurrencyRegistry, shopTradingService, shopQuantityPrompt, this::getDynamicShopConfig);
        shopLoanService = new LoanService(shopLoanStore, shopCurrencyRegistry, this::getDynamicShopConfig);
        shopEconomyStatsService = new EconomyStatsService(shopTransactionLog, shopItemRegistry, shopLoanService, this::getDynamicShopConfig);
        shopHttpServer = new DynamicShopHttpServer(this, this::getDynamicShopConfig, shopItemRegistry, shopEconomyStatsService);
        startDynamicShopTasks();
        shopHttpServer.start();
    }

    public void sendTutorial(Player player) {
        for (String line : dynamicShopConfig.tutorialLines()) {
            player.sendMessage(com.donututils.realworld.dynamicshop.gui.ShopPagedMenu.legacy(line));
        }
    }

    public DynamicShopConfig getDynamicShopConfig() {
        return dynamicShopConfig;
    }

    public Messages getMessages() {
        return shopMessages;
    }

    public ShopItemRegistry getItemRegistry() {
        return shopItemRegistry;
    }

    public PlayerDataRegistry getPlayerDataRegistry() {
        return shopPlayerDataRegistry;
    }

    public CurrencyRegistry getCurrencyRegistry() {
        return shopCurrencyRegistry;
    }

    public com.donututils.realworld.dynamicshop.service.TradingService getTradingService() {
        return shopTradingService;
    }

    public ShopGuiService getGuiService() {
        return shopGuiService;
    }

    public LoanService getLoanService() {
        return shopLoanService;
    }

    public EconomyStatsService getEconomyStatsService() {
        return shopEconomyStatsService;
    }

    private String formatShopPrice(String currencyId, double amount) {
        var provider = shopCurrencyRegistry.get(currencyId);
        return provider == null ? (amount + " " + currencyId) : provider.format(amount);
    }

    private void startDynamicShopTasks() {
        long tickTicks = Math.max(20L, dynamicShopConfig.tickIntervalSeconds() * 20L);
        long autosaveTicks = Math.max(20L, dynamicShopConfig.autosaveIntervalSeconds() * 20L);
        shopTickTask = getServer().getScheduler().runTaskTimer(this, shopPricingEngine::tick, tickTicks, tickTicks);
        shopAutosaveTask = getServer().getScheduler().runTaskTimer(this, () -> {
            shopItemRegistry.saveAll();
            shopPlayerDataRegistry.saveAll();
            shopLoanService.saveAll();
        }, autosaveTicks, autosaveTicks);
    }

    private void stopDynamicShopTasks() {
        cancel(shopTickTask);
        cancel(shopAutosaveTask);
        shopTickTask = null;
        shopAutosaveTask = null;
    }

    private DynamicShopConfig loadDynamicShopConfig() {
        FileConfiguration cfg = getConfig();
        return new DynamicShopConfig(
                cfg.getInt("dynamicshop.tick-interval-seconds", 30),
                cfg.getInt("dynamicshop.autosave-interval-seconds", 300),
                cfg.getBoolean("dynamicshop.currencies.money.enabled", true),
                cfg.getString("dynamicshop.currencies.money.display-name", "Money"),
                cfg.getBoolean("dynamicshop.currencies.shards.enabled", true),
                cfg.getString("dynamicshop.currencies.shards.display-name", "Shards"),
                cfg.getString("dynamicshop.gui.title", "&8Server Shop"),
                cfg.getBoolean("dynamicshop.features.auto-sell", true),
                cfg.getBoolean("dynamicshop.features.loans", true),
                cfg.getBoolean("dynamicshop.features.web-server", false),
                cfg.getBoolean("dynamicshop.features.gdp-stats", true),
                cfg.getBoolean("dynamicshop.features.tutorial", true),
                cfg.getBoolean("dynamicshop.features.purchase-restriction", false),
                cfg.getInt("dynamicshop.exploit-protection.transaction-cooldown-seconds", 1),
                cfg.getInt("dynamicshop.exploit-protection.global-max-quantity-per-transaction", 6400),
                cfg.getDouble("dynamicshop.loans.max-loan-amount", 10000.0),
                cfg.getDouble("dynamicshop.loans.interest-rate-percent-per-day", 5.0),
                cfg.getInt("dynamicshop.loans.max-outstanding-loans-per-player", 1),
                cfg.getString("dynamicshop.web-server.bind-address", "127.0.0.1"),
                cfg.getInt("dynamicshop.web-server.port", 8082),
                cfg.getString("dynamicshop.web-server.api-key", ""),
                cfg.getInt("dynamicshop.gdp.window-hours", 24),
                cfg.getStringList("dynamicshop.tutorial.lines")
        );
    }

    private Messages loadShopMessages() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));
        return new Messages(yaml);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MUNICIPAL
    // ═══════════════════════════════════════════════════════════════════════

    private void setupMunicipal() {
        municipalConfig = loadMunicipalConfig();

        try {
            municipalEconomy = com.donututils.realworld.municipal.economy.VaultEconomyBridge.create();
        } catch (ReflectiveOperationException | RuntimeException ex) {
            getLogger().severe("Vault's economy API doesn't look like what Municipal expects: " + ex);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (municipalEconomy == null) {
            getLogger().severe("Ledger didn't register a Vault economy - Municipal can't function. Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        municipalDatabase = new com.donututils.realworld.municipal.db.DatabaseManager(getDataFolder(), getLogger());
        locationManager = new LocationManager(this, municipalDatabase);
        permitManager = new PermitManager(this, municipalDatabase, municipalEconomy, this::getMunicipalConfig);
        courtManager = new CourtManager(this, municipalDatabase);
        jailManager = new JailManager(this, municipalDatabase, this::getMunicipalConfig, locationManager);
        claimManager = new ClaimManager(this, municipalDatabase, this::getMunicipalConfig);

        getServer().getPluginManager().registerEvents(new PermitEnforcementListener(this::getMunicipalConfig), this);
        getServer().getPluginManager().registerEvents(new JailListener(jailManager, this::getMunicipalConfig), this);
        getServer().getPluginManager().registerEvents(new PermitJoinListener(), this);
        getServer().getPluginManager().registerEvents(new ClaimProtectionListener(claimManager), this);

        registerCommand("permit", new PermitCommand(permitManager));
        registerCommand("police", new PoliceCommand(this, courtManager));
        registerCommand("court", new CourtCommand(this, courtManager, jailManager, this::getMunicipalConfig));
        registerCommand("municipal", new ClaimCommand(this, claimManager, municipalEconomy));
        registerCommand("location", new LocationCommand(locationManager));

        startMunicipalTasks();
    }

    private final class PermitJoinListener implements Listener {
        @EventHandler
        public void onJoin(PlayerJoinEvent event) {
            permitManager.reapplyOnJoin(event.getPlayer());
        }
    }

    public void reloadMunicipal() {
        reloadConfig();
        municipalConfig = loadMunicipalConfig();
    }

    public MunicipalConfig getMunicipalConfig() {
        return municipalConfig;
    }

    private void startMunicipalTasks() {
        long releaseTicks = Math.max(20L, municipalConfig.releaseCheckSeconds() * 20L);
        municipalReleaseTask = getServer().getScheduler().runTaskTimer(this, jailManager::tickReleases, releaseTicks, releaseTicks);

        long taxTicks = Math.max(20L, municipalConfig.taxTickHours() * 3600L * 20L);
        municipalPropertyTaxTask = getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> claimManager.tickPropertyTax(municipalEconomy), taxTicks, taxTicks);
    }

    private MunicipalConfig loadMunicipalConfig() {
        FileConfiguration cfg = getConfig();

        Map<String, PermitDefinition> permits = new LinkedHashMap<>();
        for (String type : new String[]{"business", "building", "weapon"}) {
            double cost = cfg.getDouble("municipal.permits." + type + ".cost", 1000.0);
            String permission = cfg.getString("municipal.permits." + type + ".permission", "municipal.permit." + type);
            permits.put(type, new PermitDefinition(type, cost, permission));
        }

        Set<String> cityLimitWorlds = new HashSet<>(cfg.getStringList("municipal.enforcement.city-limit-worlds"));
        Set<String> controlledWeapons = new HashSet<>();
        for (String material : cfg.getStringList("municipal.enforcement.controlled-weapons")) {
            controlledWeapons.add(material.toUpperCase(Locale.ROOT));
        }
        Set<String> allowedCommands = new HashSet<>();
        for (String cmd : cfg.getStringList("municipal.jail.allowed-commands")) {
            allowedCommands.add(cmd.toLowerCase(Locale.ROOT));
        }
        allowedCommands.add("help");

        return new MunicipalConfig(
                permits,
                cityLimitWorlds,
                controlledWeapons,
                cfg.getString("municipal.jail.world", "world"),
                cfg.getDouble("municipal.jail.x", 0),
                cfg.getDouble("municipal.jail.y", 100),
                cfg.getDouble("municipal.jail.z", 0),
                cfg.getDouble("municipal.jail.radius", 10.0),
                allowedCommands,
                cfg.getInt("municipal.jail.release-check-seconds", 30),
                cfg.getInt("municipal.court.max-sentence-minutes", 10080),
                cfg.getDouble("municipal.claims.claim-fee", 500.0),
                cfg.getDouble("municipal.claims.tax-per-chunk", 50.0),
                cfg.getInt("municipal.claims.tax-tick-hours", 24),
                cfg.getInt("municipal.claims.foreclosure-after-missed-ticks", 14)
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ARSENAL
    // ═══════════════════════════════════════════════════════════════════════

    private void setupArsenal() {
        arsenalConfig = loadArsenalConfig();

        arsenalKeys = new WeaponKeys(this);
        arsenalItemFactory = new WeaponItemFactory(arsenalKeys);
        arsenalWeaponManager = new WeaponManager(this, arsenalKeys, arsenalItemFactory, this::getArsenalConfig);

        getServer().getPluginManager().registerEvents(new FireListener(arsenalWeaponManager), this);
        getServer().getPluginManager().registerEvents(new ReloadListener(arsenalWeaponManager), this);

        registerCommand("arsenal", new ArsenalCommand(this, arsenalItemFactory));
    }

    public void reloadArsenal() {
        reloadConfig();
        arsenalConfig = loadArsenalConfig();
    }

    public ArsenalConfig getArsenalConfig() {
        return arsenalConfig;
    }

    private ArsenalConfig loadArsenalConfig() {
        FileConfiguration cfg = getConfig();
        Map<String, WeaponDefinition> weapons = new LinkedHashMap<>();
        ConfigurationSection weaponsSection = cfg.getConfigurationSection("arsenal.weapons");
        if (weaponsSection != null) {
            for (String id : weaponsSection.getKeys(false)) {
                ConfigurationSection s = weaponsSection.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                try {
                    org.bukkit.Material material = org.bukkit.Material.valueOf(s.getString("material", "STICK").toUpperCase(Locale.ROOT));
                    org.bukkit.Material ammoMaterial = org.bukkit.Material.valueOf(s.getString("ammo.material", "PAPER").toUpperCase(Locale.ROOT));
                    WeaponDefinition definition = new WeaponDefinition(
                            id,
                            material,
                            s.getInt("custom-model-data", 0),
                            s.getString("display-name", id),
                            s.getDouble("damage.base", 4.0),
                            s.getDouble("damage.headshot-multiplier", 2.0),
                            s.getDouble("damage.limb-multiplier", 0.75),
                            s.getInt("magazine-size", 30),
                            s.getDouble("reload-seconds", 2.0),
                            s.getLong("fire-cooldown-ms", 150),
                            s.getDouble("max-range", 50.0),
                            s.getDouble("spread.base-degrees", 1.0),
                            s.getDouble("spread.max-degrees", 6.0),
                            s.getDouble("spread.growth-per-shot-degrees", 0.5),
                            s.getDouble("spread.decay-per-second-degrees", 4.0),
                            ammoMaterial,
                            s.getString("ammo.display-name", id + " Magazine")
                    );
                    weapons.put(id.toLowerCase(Locale.ROOT), definition);
                } catch (IllegalArgumentException ex) {
                    getLogger().warning("Skipping weapon '" + id + "' - invalid material: " + ex.getMessage());
                }
            }
        }
        return new ArsenalConfig(weapons);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MOTORS
    // ═══════════════════════════════════════════════════════════════════════

    private void setupMotors() {
        motorsConfig = loadMotorsConfig();

        try {
            motorsEconomy = com.donututils.realworld.motors.economy.VaultEconomyBridge.create();
        } catch (ReflectiveOperationException | RuntimeException ex) {
            getLogger().severe("Vault's economy API doesn't look like what Motors expects: " + ex);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (motorsEconomy == null) {
            getLogger().severe("Ledger didn't register a Vault economy - Motors can't function. Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        motorsDatabase = new com.donututils.realworld.motors.db.DatabaseManager(getDataFolder(), getLogger());

        VehicleKeys keys = new VehicleKeys(this);
        VehicleItemFactory itemFactory = new VehicleItemFactory(keys);
        motorsVehicleManager = new VehicleManager(this, motorsDatabase, keys, itemFactory, this::getMotorsConfig);
        motorsVehicleManager.loadFromDatabase();

        getServer().getPluginManager().registerEvents(new PlaceListener(motorsVehicleManager, keys, this::getMotorsConfig), this);

        registerCommand("motors", new MotorsCommand(this, motorsVehicleManager, itemFactory));

        motorsTickTask = getServer().getScheduler().runTaskTimer(this, motorsVehicleManager::tick, 20L, 20L);
    }

    public void reloadMotors() {
        reloadConfig();
        motorsConfig = loadMotorsConfig();
    }

    public MotorsConfig getMotorsConfig() {
        return motorsConfig;
    }

    public com.donututils.realworld.motors.economy.VaultEconomyBridge getEconomy() {
        return motorsEconomy;
    }

    private MotorsConfig loadMotorsConfig() {
        FileConfiguration cfg = getConfig();
        Map<String, VehicleDefinition> vehicles = new LinkedHashMap<>();
        ConfigurationSection vehiclesSection = cfg.getConfigurationSection("motors.vehicles");
        if (vehiclesSection != null) {
            for (String id : vehiclesSection.getKeys(false)) {
                ConfigurationSection s = vehiclesSection.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                try {
                    VehicleDefinition.Kind kind = VehicleDefinition.Kind.valueOf(
                            s.getString("kind", "BOAT").toUpperCase(Locale.ROOT));
                    org.bukkit.Material keyMaterial = org.bukkit.Material.valueOf(s.getString("key-material", "OAK_BOAT").toUpperCase(Locale.ROOT));
                    org.bukkit.Material bodyHelmetMaterial = org.bukkit.Material.valueOf(
                            s.getString("body-helmet-material", "LEATHER_HORSE_ARMOR").toUpperCase(Locale.ROOT));

                    VehicleDefinition definition = new VehicleDefinition(
                            id,
                            kind,
                            keyMaterial,
                            s.getInt("key-custom-model-data", 0),
                            s.getString("key-display-name", id),
                            bodyHelmetMaterial,
                            s.getInt("body-helmet-custom-model-data", 0),
                            s.getString("display-name", id),
                            s.getDouble("max-fuel", 1000.0),
                            s.getDouble("fuel-drain-per-second", 0.5),
                            s.getDouble("max-wear", 100.0),
                            s.getDouble("wear-per-block", 0.02),
                            s.getDouble("refuel-cost-per-unit", 0.5),
                            s.getDouble("repair-cost-per-wear", 4.0)
                    );
                    vehicles.put(id.toLowerCase(Locale.ROOT), definition);
                } catch (IllegalArgumentException ex) {
                    getLogger().warning("Skipping vehicle '" + id + "' - invalid config: " + ex.getMessage());
                }
            }
        }
        return new MotorsConfig(vehicles);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CAREERS
    // ═══════════════════════════════════════════════════════════════════════

    private void setupCareers() {
        careersConfig = loadCareersConfig();

        try {
            careersEconomy = com.donututils.realworld.careers.economy.VaultEconomyBridge.create();
        } catch (ReflectiveOperationException | RuntimeException ex) {
            getLogger().severe("Vault's economy API doesn't look like what Careers expects: " + ex);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (careersEconomy == null) {
            getLogger().severe("Ledger didn't register a Vault economy - Careers can't function. Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        careersDatabase = new com.donututils.realworld.careers.db.DatabaseManager(getDataFolder(), getLogger());
        citizenManager = new CitizenManager(this, careersDatabase, this::getCareersConfig);
        OnboardingGuiService guiService = new OnboardingGuiService(citizenManager, this::getCareersConfig);

        getServer().getPluginManager().registerEvents(new OnboardingListener(this, citizenManager, guiService, this::getCareersConfig), this);
        getServer().getPluginManager().registerEvents(new TaskBonusListener(citizenManager, this::getCareersConfig, careersEconomy), this);

        registerCommand("career", new CareerCommand(this, citizenManager, guiService));

        careersWageTask = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> citizenManager.tickWages(careersEconomy), 20L * 60L, 20L * 60L);
    }

    public void reloadCareers() {
        reloadConfig();
        careersConfig = loadCareersConfig();
    }

    public CareersConfig getCareersConfig() {
        return careersConfig;
    }

    private CareersConfig loadCareersConfig() {
        FileConfiguration cfg = getConfig();
        Map<String, JobDefinition> jobs = new LinkedHashMap<>();
        ConfigurationSection jobsSection = cfg.getConfigurationSection("careers.jobs");
        if (jobsSection != null) {
            for (String id : jobsSection.getKeys(false)) {
                ConfigurationSection s = jobsSection.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                try {
                    AgeTier minAgeTier = AgeTier.valueOf(s.getString("min-age-tier", "ADULT").toUpperCase(Locale.ROOT));
                    Map<String, Double> blockBonuses = new LinkedHashMap<>();
                    ConfigurationSection bonusSection = s.getConfigurationSection("block-bonuses");
                    if (bonusSection != null) {
                        for (String material : bonusSection.getKeys(false)) {
                            blockBonuses.put(material.toUpperCase(Locale.ROOT), bonusSection.getDouble(material, 0.0));
                        }
                    }
                    JobDefinition job = new JobDefinition(
                            id,
                            s.getString("display-name", id),
                            minAgeTier,
                            s.getDouble("wage-amount", 25.0),
                            s.getInt("wage-interval-minutes", 30),
                            blockBonuses,
                            s.getDouble("fish-catch-bonus", 0.0),
                            s.getDouble("breed-bonus", 0.0)
                    );
                    jobs.put(id.toLowerCase(Locale.ROOT), job);
                } catch (IllegalArgumentException ex) {
                    getLogger().warning("Skipping job '" + id + "' - invalid config: " + ex.getMessage());
                }
            }
        }
        return new CareersConfig(jobs, cfg.getString("careers.onboarding.welcome-message", "&6Welcome!"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // AICHAT
    // ═══════════════════════════════════════════════════════════════════════

    private void setupAIChat() {
        aiMemory = new ConversationMemory();
        aiChatConfig = loadAIChatConfig();
        aiChatClient = buildAiChatClient(aiChatConfig);
        aiServerContext = new ServerContextService();

        registerCommand("ai", new AICommand(this, aiMemory));
    }

    public void reloadAIChat() {
        reloadConfig();
        aiChatConfig = loadAIChatConfig();
        aiChatClient = buildAiChatClient(aiChatConfig);
        aiServerContext = new ServerContextService();
    }

    public AIChatConfig getAIChatConfig() {
        return aiChatConfig;
    }

    public ChatClient getChatClient() {
        return aiChatClient;
    }

    public ServerContextService getServerContext() {
        return aiServerContext;
    }

    private ChatClient buildAiChatClient(AIChatConfig config) {
        return switch (config.provider().toLowerCase(Locale.ROOT)) {
            case "gemini" -> new GeminiApiClient(this);
            case "ollama" -> new OllamaApiClient(this, config.ollamaBaseUrl());
            default -> new ClaudeApiClient(this);
        };
    }

    private AIChatConfig loadAIChatConfig() {
        FileConfiguration cfg = getConfig();
        return new AIChatConfig(
                cfg.getString("aichat.provider", "anthropic"),
                cfg.getString("aichat.api-key", ""),
                cfg.getString("aichat.model", "claude-sonnet-5"),
                cfg.getString("aichat.ollama-base-url", "http://localhost:11434"),
                cfg.getString("aichat.assistant-name", "Astra"),
                cfg.getString("aichat.system-prompt", "You are a helpful assistant for this Minecraft server."),
                cfg.getInt("aichat.max-tokens", 400),
                cfg.getInt("aichat.memory-limit", 6),
                cfg.getInt("aichat.cooldown-seconds", 3),
                cfg.getInt("aichat.max-message-length", 400)
        );
    }
}
