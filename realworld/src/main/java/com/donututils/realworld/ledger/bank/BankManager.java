package com.donututils.realworld.ledger.bank;

import com.donututils.realworld.RealWorldPlugin;
import com.donututils.realworld.ledger.config.LedgerConfig;
import com.donututils.realworld.ledger.db.DatabaseManager;
import com.donututils.realworld.ledger.economy.LedgerEconomyProvider;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Savings accounts: a second balance, separate from the Vault-facing checking balance, that earns
 * continuously-compounding APY but isn't directly spendable - money has to be transferred back to
 * checking first. Mirrors the same in-memory-cache-plus-async-persistence approach as the checking
 * balance, since interest ticking needs frequent reads without hitting SQLite every time.
 */
public final class BankManager {

    public record TransferResult(boolean success, String message) {
    }

    private final RealWorldPlugin plugin;
    private final DatabaseManager database;
    private final LedgerEconomyProvider economy;
    private final Supplier<LedgerConfig> configSupplier;

    private final Map<UUID, Double> savingsBalances = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastInterestAtMillis = new ConcurrentHashMap<>();

    public BankManager(RealWorldPlugin plugin, DatabaseManager database, LedgerEconomyProvider economy,
                        Supplier<LedgerConfig> configSupplier) {
        this.plugin = plugin;
        this.database = database;
        this.economy = economy;
        this.configSupplier = configSupplier;
        loadAll();
    }

    private void loadAll() {
        String sql = "SELECT player_id, balance, last_interest_at FROM bank_accounts WHERE account_type = 'SAVINGS'";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                try {
                    UUID playerId = UUID.fromString(rows.getString("player_id"));
                    savingsBalances.put(playerId, rows.getDouble("balance"));
                    lastInterestAtMillis.put(playerId, rows.getLong("last_interest_at"));
                } catch (IllegalArgumentException ignored) {
                    // skip malformed row
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load savings balances", ex);
        }
    }

    public double getSavingsBalance(UUID playerId) {
        return savingsBalances.getOrDefault(playerId, 0.0);
    }

    /** Snapshot of every player with a savings balance - used by TaxManager's wealth-tax sweep. */
    public Map<UUID, Double> allSavingsBalances() {
        return Map.copyOf(savingsBalances);
    }

    /** Moves money from checking (the Vault balance) into savings. */
    public TransferResult depositToSavings(OfflinePlayer player, double amount) {
        if (amount <= 0) {
            return new TransferResult(false, "Amount must be positive.");
        }
        UUID playerId = player.getUniqueId();
        EconomyResponse withdrawal = economy.withdrawPlayer(player, amount);
        if (!withdrawal.transactionSuccess()) {
            return new TransferResult(false, withdrawal.errorMessage != null ? withdrawal.errorMessage : "Withdrawal failed.");
        }
        savingsBalances.merge(playerId, amount, Double::sum);
        lastInterestAtMillis.putIfAbsent(playerId, System.currentTimeMillis());
        persistAsync(playerId);
        return new TransferResult(true, String.format(Locale.US, "Moved $%,.2f from checking to savings.", amount));
    }

    /** Moves money from savings back into checking (the Vault balance). */
    public TransferResult withdrawFromSavings(OfflinePlayer player, double amount) {
        if (amount <= 0) {
            return new TransferResult(false, "Amount must be positive.");
        }
        UUID playerId = player.getUniqueId();
        double current = getSavingsBalance(playerId);
        if (current < amount) {
            return new TransferResult(false, String.format(Locale.US, "Your savings balance is only $%,.2f.", current));
        }
        savingsBalances.put(playerId, current - amount);
        economy.depositPlayer(player, amount);
        persistAsync(playerId);
        return new TransferResult(true, String.format(Locale.US, "Moved $%,.2f from savings to checking.", amount));
    }

    /** Runs on a repeating async task. Applies continuously-compounded interest for whatever time has
     * actually elapsed since each account's last tick, so a delayed/missed tick doesn't lose interest. */
    public void tickInterest() {
        double apyPercent = configSupplier.get().savingsApyPercent();
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Double> entry : savingsBalances.entrySet()) {
            UUID playerId = entry.getKey();
            double balance = entry.getValue();
            if (balance <= 0) {
                continue;
            }
            long last = lastInterestAtMillis.getOrDefault(playerId, now);
            double days = (now - last) / (24.0 * 60 * 60 * 1000);
            if (days <= 0) {
                continue;
            }
            double updated = balance * Math.pow(1 + apyPercent / 100.0 / 365.0, days);
            savingsBalances.put(playerId, updated);
            lastInterestAtMillis.put(playerId, now);
            persistAsync(playerId);
        }
    }

    private void persistAsync(UUID playerId) {
        double balance = getSavingsBalance(playerId);
        long lastInterest = lastInterestAtMillis.getOrDefault(playerId, System.currentTimeMillis());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO bank_accounts (player_id, account_type, balance, apy, last_interest_at) "
                    + "VALUES (?, 'SAVINGS', ?, ?, ?) "
                    + "ON CONFLICT(player_id, account_type) DO UPDATE SET balance = excluded.balance, "
                    + "apy = excluded.apy, last_interest_at = excluded.last_interest_at";
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerId.toString());
                statement.setDouble(2, balance);
                statement.setDouble(3, configSupplier.get().savingsApyPercent());
                statement.setLong(4, lastInterest);
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to persist savings balance for " + playerId, ex);
            }
        });
    }
}
