package com.donututils.realworld;

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
import com.donututils.realworld.ledger.shards.ShardManager;
import com.donututils.realworld.ledger.stock.ShareTradingManager;
import com.donututils.realworld.ledger.tax.TaxManager;

import com.donututils.realworld.market.command.ShardsCommand;
import com.donututils.realworld.market.command.ShopAdminCommand;
import com.donututils.realworld.market.command.ShopCommand;
import com.donututils.realworld.market.config.BoutiqueItem;
import com.donututils.realworld.market.config.CatalogEntry;
import com.donututils.realworld.market.config.MarketConfig;
import com.donututils.realworld.market.catalog.ItemCatalog;
import com.donututils.realworld.market.gui.MarketGuiService;
import com.donututils.realworld.market.gui.MarketMenuClickListener;
import com.donututils.realworld.market.pricing.MarketPriceStore;
import com.donututils.realworld.market.pricing.MarketPricingEngine;
import com.donututils.realworld.market.service.MarketService;

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
    private ShardManager shardManager;
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
    private VehicleItemFactory motorsItemFactory;
    private VehicleManager motorsVehicleManager;
    private volatile MotorsConfig motorsConfig;
    private BukkitTask motorsTickTask;

    // ── Market ───────────────────────────────────────────────────────────────
    private java.util.List<CatalogEntry> marketCatalog;
    private MarketPriceStore marketPriceStore;
    private MarketPricingEngine marketPricingEngine;
    private MarketService marketService;
    private MarketGuiService marketGuiService;
    private volatile MarketConfig marketConfig;
    private BukkitTask marketPriceTickTask;
    private BukkitTask marketAutosaveTask;

    // ── Careers ──────────────────────────────────────────────────────────────
    private com.donututils.realworld.careers.db.DatabaseManager careersDatabase;
    private com.donututils.realworld.careers.economy.VaultEconomyBridge careersEconomy;
    private CitizenManager citizenManager;
    private volatile CareersConfig careersConfig;
    private BukkitTask careersWageTask;
    private BukkitTask careersPlaytimeTask;

    // ── Teams ────────────────────────────────────────────────────────────────
    private com.donututils.realworld.teams.db.DatabaseManager teamsDatabase;
    private com.donututils.realworld.teams.TeamManager teamManager;

    // ── Crates ───────────────────────────────────────────────────────────────
    private com.donututils.realworld.crates.db.DatabaseManager cratesDatabase;
    private com.donututils.realworld.crates.CrateManager crateManager;
    private volatile com.donututils.realworld.crates.CrateConfig crateConfig;

    // ── Staff panel ──────────────────────────────────────────────────────────
    private com.donututils.realworld.staff.FreezeManager freezeManager;

    // ── EconomyWatchdog / MarketWatch / PurchaseAlert (Discord addons) ───────
    private com.donututils.realworld.ecowatch.discord.DiscordWebhook ecoWatchWebhook;
    private BukkitTask ecoWatchTask;
    private com.donututils.realworld.marketwatch.discord.DiscordWebhook marketWatchWebhook;
    private com.donututils.realworld.purchasealert.discord.DiscordWebhook purchaseAlertWebhook;
    private BukkitTask purchaseAlertTask;

    // ── PunishmentHistoryGUI ─────────────────────────────────────────────────
    private com.donututils.realworld.punishhistory.db.DatabaseManager punishHistoryDatabase;
    private com.donututils.realworld.punishhistory.NoteStore noteStore;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "stocks.yml").exists()) {
            saveResource("stocks.yml", false);
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
        setupMunicipal();
        setupArsenal();
        setupMotors();
        setupMarket();
        setupCareers();
        setupTeams();
        setupCrates();
        setupStaff();
        setupEcoWatch();
        setupMarketWatch();
        setupPunishHistory();
        setupPurchaseAlert();
        registerCommand("help", new com.donututils.realworld.help.HelpCommand());

        getLogger().info("RealWorld enabled - Ledger, StockMarket, Market, Municipal, Arsenal, Motors, Careers, "
                + "Teams, Crates, staff tools, and every watchdog/alert addon are all live.");
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
        cancel(careersPlaytimeTask);
        if (careersDatabase != null) {
            careersDatabase.shutdown();
        }

        cancel(marketPriceTickTask);
        cancel(marketAutosaveTask);
        if (marketPricingEngine != null) {
            marketPricingEngine.saveAll();
        }

        if (cratesDatabase != null) {
            cratesDatabase.shutdown();
        }
        if (teamsDatabase != null) {
            teamsDatabase.shutdown();
        }
        if (punishHistoryDatabase != null) {
            punishHistoryDatabase.shutdown();
        }
        cancel(ecoWatchTask);
        cancel(purchaseAlertTask);
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
        shardManager = new ShardManager(this, ledgerDatabase);

        getServer().getServicesManager().register(Economy.class, economyProvider, this, ServicePriority.Highest);
        getServer().getPluginManager().registerEvents(new LedgerJoinListener(), this);

        registerCommand("bank", new BankCommand(this, economyProvider, bankManager, creditScoreManager));
        registerCommand("loan", new LoanCommand(this, loanManager));
        registerCommand("corp", new CorpCommand(this, corporationManager, shareTradingManager));
        registerCommand("stock", new com.donututils.realworld.ledger.command.StockCommand(this, corporationManager, shareTradingManager, taxManager, economyProvider));
        registerCommand("ledgeradmin", new LedgerAdminCommand(this, loanManager));
        registerCommand("pay", new com.donututils.realworld.ledger.command.PayCommand(economyProvider));
        registerCommand("addshards", new com.donututils.realworld.ledger.command.AddShardsCommand(shardManager));
        registerCommand("removeshards", new com.donututils.realworld.ledger.command.RemoveShardsCommand(shardManager));

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

    public ShardManager getShardManager() {
        return shardManager;
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
                            s.getString("ammo.display-name", id + " Magazine"),
                            s.getDouble("splash-radius", 0.0),
                            s.getDouble("price-money", 500.0),
                            s.getDouble("ammo.price-money", 25.0)
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
        motorsItemFactory = new VehicleItemFactory(keys);
        motorsVehicleManager = new VehicleManager(this, motorsDatabase, keys, motorsItemFactory, this::getMotorsConfig);
        motorsVehicleManager.loadFromDatabase();

        getServer().getPluginManager().registerEvents(new PlaceListener(motorsVehicleManager, keys, this::getMotorsConfig), this);

        registerCommand("motors", new MotorsCommand(this, motorsVehicleManager, motorsItemFactory));

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
                            s.getDouble("repair-cost-per-wear", 4.0),
                            s.getDouble("price-money", 2000.0)
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
    // MARKET
    // ═══════════════════════════════════════════════════════════════════════

    private void setupMarket() {
        marketConfig = loadMarketConfig();
        marketCatalog = ItemCatalog.generate();
        marketPriceStore = new MarketPriceStore(getDataFolder(), getLogger());
        marketPricingEngine = new MarketPricingEngine(marketPriceStore, this::getMarketConfig);
        marketService = new MarketService(marketCatalog, marketPricingEngine, economyProvider, shardManager,
                this::getMarketConfig, this::getArsenalConfig, arsenalItemFactory, this::getMotorsConfig,
                motorsItemFactory, permitManager);
        marketGuiService = new MarketGuiService(marketCatalog, marketPricingEngine, marketService, this::getMarketConfig,
                this::getArsenalConfig, this::getMotorsConfig, this::getMunicipalConfig);

        getServer().getPluginManager().registerEvents(new MarketMenuClickListener(), this);

        registerCommand("shop", new ShopCommand(marketGuiService, marketService));
        registerCommand("shopadmin", new ShopAdminCommand(this, shardManager));
        registerCommand("shards", new ShardsCommand(shardManager));

        startMarketTasks();
        getLogger().info("Market enabled with " + marketCatalog.size() + " catalog item(s) generated.");
    }

    public void reloadMarket() {
        reloadConfig();
        marketConfig = loadMarketConfig();
        stopMarketTasks();
        startMarketTasks();
    }

    public MarketConfig getMarketConfig() {
        return marketConfig;
    }

    private void startMarketTasks() {
        long tickTicks = Math.max(20L, marketConfig.priceTickIntervalSeconds() * 20L);
        marketPriceTickTask = getServer().getScheduler().runTaskTimerAsynchronously(this, marketPricingEngine::tick, tickTicks, tickTicks);
        marketAutosaveTask = getServer().getScheduler().runTaskTimer(this, marketPricingEngine::saveAll, 20L * 60L * 5L, 20L * 60L * 5L);
    }

    private void stopMarketTasks() {
        cancel(marketPriceTickTask);
        cancel(marketAutosaveTask);
        marketPriceTickTask = null;
        marketAutosaveTask = null;
    }

    private MarketConfig loadMarketConfig() {
        FileConfiguration cfg = getConfig();

        java.util.Map<String, Double> multipliers = new LinkedHashMap<>();
        ConfigurationSection multSection = cfg.getConfigurationSection("market.category-multipliers");
        if (multSection != null) {
            for (String key : multSection.getKeys(false)) {
                multipliers.put(key, multSection.getDouble(key, 1.0));
            }
        }

        java.util.Map<String, BoutiqueItem> boutique = new LinkedHashMap<>();
        ConfigurationSection boutiqueSection = cfg.getConfigurationSection("market.shard-boutique");
        if (boutiqueSection != null) {
            for (String id : boutiqueSection.getKeys(false)) {
                ConfigurationSection s = boutiqueSection.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                boutique.put(id, new BoutiqueItem(
                        id,
                        s.getString("display-name", id),
                        s.getString("description", ""),
                        s.contains("base-weapon") ? s.getString("base-weapon", null) : null,
                        s.contains("base-vehicle") ? s.getString("base-vehicle", null) : null,
                        s.contains("material") ? s.getString("material", null) : null,
                        s.getInt("custom-model-data", 0),
                        s.getLong("price-shards", 100)
                ));
            }
        }

        return new MarketConfig(
                cfg.getString("market.gui.title", "&8Server Market"),
                multipliers,
                cfg.getDouble("market.sell-back-fraction", 0.4),
                cfg.getInt("market.transaction-cooldown-seconds", 1),
                cfg.getInt("market.max-quantity-per-transaction", 6400),
                boutique,
                cfg.getInt("market.dynamic-pricing.tick-interval-seconds", 60),
                cfg.getDouble("market.dynamic-pricing.buy-impact-percent", 0.05),
                cfg.getDouble("market.dynamic-pricing.sell-impact-percent", 0.05),
                cfg.getDouble("market.dynamic-pricing.decay-percent-per-tick", 2.0),
                cfg.getDouble("market.dynamic-pricing.min-price-factor", 0.25),
                cfg.getDouble("market.dynamic-pricing.max-price-factor", 4.0)
        );
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

        registerCommand("career", new CareerCommand(this, citizenManager, guiService, economyProvider, stockPortfolioService, shardManager));

        careersWageTask = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> citizenManager.tickWages(careersEconomy), 20L * 60L, 20L * 60L);
        careersPlaytimeTask = getServer().getScheduler().runTaskTimer(this,
                () -> citizenManager.tickPlaytime(new java.util.ArrayList<>(getServer().getOnlinePlayers())), 20L * 60L, 20L * 60L);
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
        return new CareersConfig(
                jobs,
                cfg.getString("careers.onboarding.welcome-message", "&6Welcome!"),
                cfg.getInt("careers.school.minor-to-adult-playtime-minutes", 600),
                cfg.getDouble("careers.legacy.inheritance-percent", 10.0),
                cfg.getLong("careers.legacy.shard-bonus-per-legacy", 50),
                cfg.getDouble("careers.legacy.wage-bonus-percent-per-legacy", 2.0)
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TEAMS
    // ═══════════════════════════════════════════════════════════════════════

    private void setupTeams() {
        teamsDatabase = new com.donututils.realworld.teams.db.DatabaseManager(getDataFolder(), getLogger());
        teamManager = new com.donututils.realworld.teams.TeamManager(this, teamsDatabase);

        registerCommand("team", new com.donututils.realworld.teams.TeamCommand(teamManager));
        registerCommand("teambaltop", new com.donututils.realworld.teams.TeamLeaderboardCommand(
                teamManager, economyProvider, shardManager, com.donututils.realworld.teams.TeamLeaderboardCommand.Stat.MONEY));
        registerCommand("teamshardstop", new com.donututils.realworld.teams.TeamLeaderboardCommand(
                teamManager, economyProvider, shardManager, com.donututils.realworld.teams.TeamLeaderboardCommand.Stat.SHARDS));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CRATES
    // ═══════════════════════════════════════════════════════════════════════

    private void setupCrates() {
        crateConfig = loadCrateConfig();
        cratesDatabase = new com.donututils.realworld.crates.db.DatabaseManager(getDataFolder(), getLogger());
        com.donututils.realworld.crates.CrateKeys crateKeys = new com.donututils.realworld.crates.CrateKeys(this);
        com.donututils.realworld.crates.CrateItemFactory crateItemFactory = new com.donututils.realworld.crates.CrateItemFactory(crateKeys);
        crateManager = new com.donututils.realworld.crates.CrateManager(this, cratesDatabase, crateKeys, crateItemFactory,
                economyProvider, shardManager, this::getCrateConfig);
        crateManager.respawnHolograms();

        getServer().getPluginManager().registerEvents(new com.donututils.realworld.crates.CrateInteractListener(crateManager), this);
        registerCommand("crate", new com.donututils.realworld.crates.CrateCommand(this, crateManager));
    }

    public void reloadCrates() {
        reloadConfig();
        crateConfig = loadCrateConfig();
    }

    public com.donututils.realworld.crates.CrateConfig getCrateConfig() {
        return crateConfig;
    }

    private com.donututils.realworld.crates.CrateConfig loadCrateConfig() {
        FileConfiguration cfg = getConfig();
        Map<String, com.donututils.realworld.crates.CrateDefinition> crates = new LinkedHashMap<>();
        ConfigurationSection cratesSection = cfg.getConfigurationSection("crates");
        if (cratesSection != null) {
            for (String id : cratesSection.getKeys(false)) {
                ConfigurationSection s = cratesSection.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                try {
                    org.bukkit.Material keyMaterial = org.bukkit.Material.valueOf(s.getString("key-material", "TRIPWIRE_HOOK").toUpperCase(Locale.ROOT));
                    List<com.donututils.realworld.crates.CrateReward> rewards = new ArrayList<>();
                    ConfigurationSection rewardsSection = s.getConfigurationSection("rewards");
                    if (rewardsSection != null) {
                        for (String rewardId : rewardsSection.getKeys(false)) {
                            ConfigurationSection r = rewardsSection.getConfigurationSection(rewardId);
                            if (r == null) {
                                continue;
                            }
                            try {
                                com.donututils.realworld.crates.CrateReward.Kind kind =
                                        com.donututils.realworld.crates.CrateReward.Kind.valueOf(r.getString("kind", "ITEM").toUpperCase(Locale.ROOT));
                                org.bukkit.Material material = kind == com.donututils.realworld.crates.CrateReward.Kind.ITEM
                                        ? org.bukkit.Material.valueOf(r.getString("material", "STONE").toUpperCase(Locale.ROOT)) : null;
                                rewards.add(new com.donututils.realworld.crates.CrateReward(
                                        kind, material,
                                        r.getDouble("money-amount", 0.0),
                                        r.getLong("shards-amount", 0),
                                        r.getInt("item-amount-min", 1),
                                        r.getInt("item-amount-max", 1),
                                        r.getString("display-name", rewardId),
                                        r.getInt("weight", 1)
                                ));
                            } catch (IllegalArgumentException ex) {
                                getLogger().warning("Skipping reward '" + rewardId + "' in crate '" + id + "' - invalid config: " + ex.getMessage());
                            }
                        }
                    }
                    crates.put(id.toLowerCase(Locale.ROOT), new com.donututils.realworld.crates.CrateDefinition(
                            id, s.getString("display-name", id), keyMaterial, s.getInt("key-custom-model-data", 0), rewards));
                } catch (IllegalArgumentException ex) {
                    getLogger().warning("Skipping crate '" + id + "' - invalid config: " + ex.getMessage());
                }
            }
        }
        return new com.donututils.realworld.crates.CrateConfig(crates);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STAFF PANEL (/sus)
    // ═══════════════════════════════════════════════════════════════════════

    private void setupStaff() {
        freezeManager = new com.donututils.realworld.staff.FreezeManager();
        com.donututils.realworld.staff.StaffPanel staffPanel = new com.donututils.realworld.staff.StaffPanel(freezeManager);

        getServer().getPluginManager().registerEvents(new com.donututils.realworld.staff.FreezeListener(freezeManager), this);
        getServer().getPluginManager().registerEvents(new com.donututils.realworld.staff.gui.StaffMenuClickListener(), this);
        registerCommand("sus", new com.donututils.realworld.staff.SusCommand(staffPanel));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ECOWATCH
    // ═══════════════════════════════════════════════════════════════════════

    private void setupEcoWatch() {
        ecoWatchWebhook = new com.donututils.realworld.ecowatch.discord.DiscordWebhook(this);
        startEcoWatchTask();
        registerCommand("ecowatch", new com.donututils.realworld.ecowatch.EcoWatchCommand(this, ecoWatchWebhook));
    }

    public void reloadEcoWatch() {
        reloadConfig();
        cancel(ecoWatchTask);
        startEcoWatchTask();
    }

    private void startEcoWatchTask() {
        FileConfiguration cfg = getConfig();
        ecoWatchWebhook.configure(cfg.getString("ecowatch.discord.webhook-url", ""), cfg.getString("ecowatch.discord.username", "EconomyWatchdog"));
        double threshold = cfg.getDouble("ecowatch.balance-jump-threshold", 10000.0);
        com.donututils.realworld.ecowatch.BalanceWatcher watcher =
                new com.donututils.realworld.ecowatch.BalanceWatcher(economyProvider, ecoWatchWebhook, () -> threshold);
        long intervalTicks = Math.max(20L, cfg.getInt("ecowatch.check-interval-seconds", 60) * 20L);
        ecoWatchTask = getServer().getScheduler().runTaskTimerAsynchronously(this, watcher, intervalTicks, intervalTicks);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MARKETWATCH
    // ═══════════════════════════════════════════════════════════════════════

    private void setupMarketWatch() {
        marketWatchWebhook = new com.donututils.realworld.marketwatch.discord.DiscordWebhook(this);
        marketWatchWebhook.configure(getConfig().getString("marketwatch.discord.webhook-url", ""),
                getConfig().getString("marketwatch.discord.username", "MarketWatch"));
        com.donututils.realworld.marketwatch.MarketWatchCommand marketWatchCommand =
                new com.donututils.realworld.marketwatch.MarketWatchCommand(this, marketWatchWebhook, stockRegistry);
        registerCommand("marketwatch", marketWatchCommand);
        registerCommand("ahstats", new com.donututils.realworld.marketwatch.AhStatsCommand(marketWatchCommand));
    }

    public void reloadMarketWatch() {
        reloadConfig();
        marketWatchWebhook.configure(getConfig().getString("marketwatch.discord.webhook-url", ""),
                getConfig().getString("marketwatch.discord.username", "MarketWatch"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUNISHMENT HISTORY / STAFF NOTES
    // ═══════════════════════════════════════════════════════════════════════

    private void setupPunishHistory() {
        punishHistoryDatabase = new com.donututils.realworld.punishhistory.db.DatabaseManager(getDataFolder(), getLogger());
        noteStore = new com.donututils.realworld.punishhistory.NoteStore(punishHistoryDatabase, getLogger());

        registerCommand("punishhistory", new com.donututils.realworld.punishhistory.PunishHistoryCommand(courtManager, noteStore));
        registerCommand("note", new com.donututils.realworld.punishhistory.NoteCommand(noteStore));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PURCHASE ALERT
    // ═══════════════════════════════════════════════════════════════════════

    private void setupPurchaseAlert() {
        purchaseAlertWebhook = new com.donututils.realworld.purchasealert.discord.DiscordWebhook(this);
        startPurchaseAlertTask();
        registerCommand("purchasealert", new com.donututils.realworld.purchasealert.command.PurchaseAlertCommand(this));
    }

    public void reloadPurchaseAlert() {
        reloadConfig();
        cancel(purchaseAlertTask);
        startPurchaseAlertTask();
    }

    public com.donututils.realworld.purchasealert.discord.DiscordWebhook getPurchaseAlertWebhook() {
        return purchaseAlertWebhook;
    }

    private void startPurchaseAlertTask() {
        FileConfiguration cfg = getConfig();
        purchaseAlertWebhook.configure(cfg.getString("purchasealert.discord.webhook-url", ""),
                cfg.getString("purchasealert.discord.username", "Store Purchases"));

        String backendUrl = cfg.getString("purchasealert.backend_url", "");
        String pluginKey = cfg.getString("purchasealert.plugin_key", "");
        if (backendUrl.isBlank() || pluginKey.isBlank()) {
            getLogger().warning("purchasealert.backend_url or purchasealert.plugin_key is not set - copy the same "
                    + "two values from StoreBridge's own config.yml. Purchase polling is paused until then.");
            return;
        }
        int orderLimit = Math.max(1, Math.min(25, cfg.getInt("purchasealert.order_limit", 10)));
        com.donututils.realworld.purchasealert.http.BackendOrdersClient client =
                new com.donututils.realworld.purchasealert.http.BackendOrdersClient(backendUrl, pluginKey);
        com.donututils.realworld.purchasealert.config.AlertConfig alertConfig = new com.donututils.realworld.purchasealert.config.AlertConfig(
                backendUrl, pluginKey, Math.max(5, cfg.getInt("purchasealert.poll_interval_seconds", 15)), orderLimit,
                cfg.getString("purchasealert.discord.webhook-url", ""), cfg.getString("purchasealert.discord.username", "Store Purchases"));
        com.donututils.realworld.purchasealert.watch.PurchaseWatcher watcher =
                new com.donututils.realworld.purchasealert.watch.PurchaseWatcher(this, client, purchaseAlertWebhook, alertConfig);
        long intervalTicks = Math.max(20L, alertConfig.pollIntervalSeconds() * 20L);
        purchaseAlertTask = getServer().getScheduler().runTaskTimerAsynchronously(this, watcher, intervalTicks, intervalTicks);
    }
}
