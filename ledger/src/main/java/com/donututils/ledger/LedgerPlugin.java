package com.donututils.ledger;

import com.donututils.ledger.bank.BankManager;
import com.donututils.ledger.command.BankCommand;
import com.donututils.ledger.command.CorpCommand;
import com.donututils.ledger.command.LedgerAdminCommand;
import com.donututils.ledger.command.LoanCommand;
import com.donututils.ledger.command.StockCommand;
import com.donututils.ledger.config.LedgerConfig;
import com.donututils.ledger.config.LoanTier;
import com.donututils.ledger.corp.CorporationManager;
import com.donututils.ledger.credit.CreditScoreManager;
import com.donututils.ledger.db.DatabaseManager;
import com.donututils.ledger.economy.LedgerEconomyProvider;
import com.donututils.ledger.loan.LoanManager;
import com.donututils.ledger.stock.ShareTradingManager;
import com.donututils.ledger.tax.TaxManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Ledger: a from-scratch banking, credit, loan, and corporate stock market economy. Unlike every
 * other plugin in this repo, this one does NOT read an existing Vault economy - it implements
 * {@link Economy} itself and registers with Vault's ServicesManager, becoming the server's actual
 * money. Vault still needs to be installed (that's how other plugins find a registered economy), but
 * no other economy plugin is required or consulted.
 */
public final class LedgerPlugin extends JavaPlugin implements Listener {

    private DatabaseManager databaseManager;
    private LedgerEconomyProvider economyProvider;
    private BankManager bankManager;
    private CreditScoreManager creditScoreManager;
    private LoanManager loanManager;
    private CorporationManager corporationManager;
    private ShareTradingManager shareTradingManager;
    private TaxManager taxManager;

    private volatile LedgerConfig config;

    private final List<BukkitTask> scheduledTasks = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = loadConfigValues();

        databaseManager = new DatabaseManager(getDataFolder(), getLogger());
        economyProvider = new LedgerEconomyProvider(this, databaseManager, config.startingBalance());
        creditScoreManager = new CreditScoreManager(this, databaseManager, this::getLedgerConfig);
        bankManager = new BankManager(this, databaseManager, economyProvider, this::getLedgerConfig);
        loanManager = new LoanManager(this, databaseManager, economyProvider, creditScoreManager, this::getLedgerConfig);
        corporationManager = new CorporationManager(this, databaseManager, economyProvider, this::getLedgerConfig);
        shareTradingManager = new ShareTradingManager(this, databaseManager, economyProvider, corporationManager, this::getLedgerConfig);
        taxManager = new TaxManager(this, economyProvider, bankManager, corporationManager, this::getLedgerConfig);

        getServer().getServicesManager().register(Economy.class, economyProvider, this, ServicePriority.Highest);
        getServer().getPluginManager().registerEvents(this, this);

        registerCommand("bank", new BankCommand(this, economyProvider, bankManager, creditScoreManager));
        registerCommand("loan", new LoanCommand(this, loanManager));
        registerCommand("corp", new CorpCommand(this, corporationManager, shareTradingManager));
        registerCommand("stock", new StockCommand(this, corporationManager, shareTradingManager, taxManager, economyProvider));
        registerCommand("ledgeradmin", new LedgerAdminCommand(this, loanManager));

        startTasks();

        getLogger().info("Ledger enabled - registered as the server's Vault economy provider.");
    }

    @Override
    public void onDisable() {
        stopTasks();
        getServer().getServicesManager().unregisterAll(this);
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        economyProvider.ensureAccount(event.getPlayer().getUniqueId());
    }

    public void reloadLedger() {
        reloadConfig();
        config = loadConfigValues();
        stopTasks();
        startTasks();
    }

    public LedgerConfig getLedgerConfig() {
        return config;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public LedgerEconomyProvider getEconomyProvider() {
        return economyProvider;
    }

    private void startTasks() {
        long tickMinutesToTicks = 20L * 60L;
        scheduledTasks.add(getServer().getScheduler().runTaskTimerAsynchronously(this, bankManager::tickInterest,
                config.interestTickMinutes() * tickMinutesToTicks, config.interestTickMinutes() * tickMinutesToTicks));
        scheduledTasks.add(getServer().getScheduler().runTaskTimerAsynchronously(this, loanManager::tickLoans,
                config.loanPaymentTickMinutes() * tickMinutesToTicks, config.loanPaymentTickMinutes() * tickMinutesToTicks));
        scheduledTasks.add(getServer().getScheduler().runTaskTimerAsynchronously(this, shareTradingManager::tickPrices,
                config.corpPriceTickMinutes() * tickMinutesToTicks, config.corpPriceTickMinutes() * tickMinutesToTicks));

        long tickHoursToTicks = 20L * 60L * 60L;
        scheduledTasks.add(getServer().getScheduler().runTaskTimerAsynchronously(this, taxManager::collectWealthTax,
                config.wealthTaxTickHours() * tickHoursToTicks, config.wealthTaxTickHours() * tickHoursToTicks));
        scheduledTasks.add(getServer().getScheduler().runTaskTimerAsynchronously(this, taxManager::collectCorporateTax,
                config.corporateTaxTickHours() * tickHoursToTicks, config.corporateTaxTickHours() * tickHoursToTicks));
    }

    private void stopTasks() {
        for (BukkitTask task : scheduledTasks) {
            task.cancel();
        }
        scheduledTasks.clear();
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        } else {
            getLogger().warning("plugin.yml is missing the '" + name + "' command definition.");
        }
    }

    private LedgerConfig loadConfigValues() {
        FileConfiguration cfg = getConfig();

        List<LoanTier> tiers = new ArrayList<>();
        for (Map<?, ?> raw : cfg.getMapList("loans.tiers")) {
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
                cfg.getDouble("starting-balance", 1000.0),
                cfg.getDouble("bank.savings-apy-percent", 4.0),
                cfg.getInt("bank.interest-tick-minutes", 15),
                cfg.getInt("credit.starting-score", 650),
                cfg.getInt("credit.points-per-on-time-payment", 5),
                cfg.getInt("credit.points-per-missed-payment", -25),
                cfg.getInt("credit.points-per-default", -100),
                tiers,
                cfg.getInt("loans.term-days", 30),
                cfg.getInt("loans.missed-payments-before-default", 3),
                cfg.getInt("loans.payment-tick-minutes", 15),
                cfg.getDouble("corporations.founding-cost", 2500.0),
                cfg.getInt("corporations.default-total-shares", 1000),
                cfg.getDouble("corporations.base-drift", 0.0001),
                cfg.getDouble("corporations.base-volatility", 0.01),
                cfg.getDouble("corporations.profit-price-sensitivity", 0.5),
                cfg.getInt("corporations.price-tick-minutes", 5),
                cfg.getDouble("tax.wealth-tax-percent", 1.0),
                cfg.getInt("tax.wealth-tax-tick-hours", 24),
                cfg.getDouble("tax.transaction-tax-percent", 2.0),
                cfg.getDouble("tax.corporate-tax-percent", 5.0),
                cfg.getInt("tax.corporate-tax-tick-hours", 24),
                cfg.getBoolean("tax.expense-writeoff-enabled", true),
                cfg.getDouble("tax.trust-shield-percent", 40.0),
                cfg.getDouble("tax.trust-setup-cost", 10000.0)
        );
    }
}
