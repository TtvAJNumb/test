package com.donututils.realworld.ledger.loan;

import com.donututils.realworld.RealWorldPlugin;
import com.donututils.realworld.ledger.config.LedgerConfig;
import com.donututils.realworld.ledger.config.LoanTier;
import com.donututils.realworld.ledger.credit.CreditScoreManager;
import com.donututils.realworld.ledger.db.DatabaseManager;
import com.donututils.realworld.ledger.economy.LedgerEconomyProvider;
import com.donututils.realworld.ledger.model.CreditProfile;
import com.donututils.realworld.ledger.model.Loan;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Loan origination (gated by credit score, via {@link LedgerConfig#tierFor}), servicing (interest
 * accrual, repayment), and default handling. One active loan per player at a time.
 * <p>
 * Thread contract: {@link #repay} is main-thread-safe (its only blocking work is wrapped in an async
 * task internally). {@link #originate} blocks synchronously on a database insert to get the new
 * loan's id back before returning - callers MUST invoke it from an async task
 * (e.g. {@code Bukkit.getScheduler().runTaskAsynchronously}), never directly from a command handler.
 */
public final class LoanManager {

    public record LoanResult(boolean success, String message) {
    }

    private final RealWorldPlugin plugin;
    private final DatabaseManager database;
    private final LedgerEconomyProvider economy;
    private final CreditScoreManager creditScoreManager;
    private final Supplier<LedgerConfig> configSupplier;

    private final Map<UUID, Loan> activeLoans = new ConcurrentHashMap<>();

    public LoanManager(RealWorldPlugin plugin, DatabaseManager database, LedgerEconomyProvider economy,
                        CreditScoreManager creditScoreManager, Supplier<LedgerConfig> configSupplier) {
        this.plugin = plugin;
        this.database = database;
        this.economy = economy;
        this.creditScoreManager = creditScoreManager;
        this.configSupplier = configSupplier;
        loadActiveLoans();
    }

    private void loadActiveLoans() {
        String sql = "SELECT id, player_id, principal, remaining_balance, apr, term_days, issued_at, next_payment_due, missed_payments "
                + "FROM loans WHERE status = 'ACTIVE'";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                try {
                    UUID playerId = UUID.fromString(rows.getString("player_id"));
                    Loan loan = new Loan(rows.getLong("id"), rows.getDouble("principal"), rows.getDouble("apr"),
                            rows.getInt("term_days"), rows.getLong("issued_at"));
                    loan.setRemainingBalance(rows.getDouble("remaining_balance"));
                    for (int i = 0; i < rows.getInt("missed_payments"); i++) {
                        loan.incrementMissedPayments();
                    }
                    activeLoans.put(playerId, loan);
                } catch (IllegalArgumentException ignored) {
                    // skip malformed row
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load active loans", ex);
        }
    }

    public Loan getActiveLoan(UUID playerId) {
        Loan loan = activeLoans.get(playerId);
        if (loan != null) {
            loan.accrue(System.currentTimeMillis());
        }
        return loan;
    }

    public LoanResult originate(OfflinePlayer player, double amount) {
        UUID playerId = player.getUniqueId();
        if (activeLoans.containsKey(playerId)) {
            return new LoanResult(false, "You already have an active loan - repay it first with /loan repay <amount>.");
        }
        if (amount <= 0) {
            return new LoanResult(false, "Loan amount must be positive.");
        }
        CreditProfile profile = creditScoreManager.getCached(playerId);
        LedgerConfig config = configSupplier.get();
        LoanTier tier = config.tierFor(profile.score());
        if (amount > tier.maxLoanAmount()) {
            return new LoanResult(false, String.format(Locale.US,
                    "Your credit score (%d) qualifies you for up to $%,.2f. Try a smaller amount.",
                    profile.score(), tier.maxLoanAmount()));
        }

        Loan loan = new Loan(-1, amount, tier.aprPercent(), config.loanTermDays(), System.currentTimeMillis());
        long id = insert(playerId, loan);
        if (id < 0) {
            return new LoanResult(false, "Something went wrong setting up the loan - try again.");
        }
        loan.setId(id);
        activeLoans.put(playerId, loan);
        economy.depositPlayer(player, amount);

        return new LoanResult(true, String.format(Locale.US,
                "Approved! Borrowed $%,.2f at %.1f%% APR (your credit score: %d), due in %d days.",
                amount, tier.aprPercent(), profile.score(), config.loanTermDays()));
    }

    public LoanResult repay(OfflinePlayer player, double amount) {
        UUID playerId = player.getUniqueId();
        Loan loan = activeLoans.get(playerId);
        if (loan == null) {
            return new LoanResult(false, "You don't have an active loan.");
        }
        if (amount <= 0) {
            return new LoanResult(false, "Amount must be positive.");
        }
        loan.accrue(System.currentTimeMillis());
        double payment = Math.min(amount, loan.remainingBalance());
        EconomyResponse withdrawal = economy.withdrawPlayer(player, payment);
        if (!withdrawal.transactionSuccess()) {
            return new LoanResult(false, withdrawal.errorMessage != null ? withdrawal.errorMessage : "You don't have that much.");
        }
        loan.setRemainingBalance(loan.remainingBalance() - payment);
        creditScoreManager.recordOnTimePayment(playerId);

        String message;
        if (loan.isPaidOff()) {
            loan.setStatus(Loan.Status.PAID);
            activeLoans.remove(playerId);
            message = "Paid off your loan in full!";
        } else {
            message = String.format(Locale.US, "Paid $%,.2f - $%,.2f remaining.", payment, loan.remainingBalance());
        }
        update(playerId, loan);
        return new LoanResult(true, message);
    }

    /** Admin action: clears a player's active loan without affecting their credit score either way. */
    public boolean forgiveLoan(UUID playerId) {
        Loan loan = activeLoans.remove(playerId);
        if (loan == null) {
            return false;
        }
        loan.setRemainingBalance(0);
        loan.setStatus(Loan.Status.PAID);
        update(playerId, loan);
        return true;
    }

    /** Runs on a repeating async task: accrues interest on every active loan, and handles any loan
     * that has reached its due date without being fully repaid - a missed payment, or a full default
     * (with an attempted checking-account seizure) past the configured missed-payment threshold. */
    public void tickLoans() {
        LedgerConfig config = configSupplier.get();
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Loan> entry : activeLoans.entrySet()) {
            UUID playerId = entry.getKey();
            Loan loan = entry.getValue();
            loan.accrue(now);

            if (now < loan.nextPaymentDueMillis() || loan.isPaidOff()) {
                update(playerId, loan);
                continue;
            }

            loan.incrementMissedPayments();
            creditScoreManager.recordMissedPayment(playerId);
            loan.advanceNextPaymentDue();

            if (loan.missedPayments() >= config.missedPaymentsBeforeDefault()) {
                handleDefault(playerId, loan);
            } else {
                update(playerId, loan);
            }
        }
    }

    private void handleDefault(UUID playerId, Loan loan) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
        double owed = loan.remainingBalance();
        double available = economy.getBalance(offlinePlayer);
        double seized = Math.min(available, owed);
        if (seized > 0) {
            economy.withdrawPlayer(offlinePlayer, seized);
        }
        creditScoreManager.recordDefault(playerId);
        loan.setRemainingBalance(0);
        loan.setStatus(Loan.Status.DEFAULTED);
        activeLoans.remove(playerId);
        update(playerId, loan);
        plugin.getLogger().info("Loan #" + loan.id() + " for " + playerId + " defaulted - seized $"
                + String.format(Locale.US, "%,.2f", seized) + " of $" + String.format(Locale.US, "%,.2f", owed) + " owed.");
    }

    private long insert(UUID playerId, Loan loan) {
        String sql = "INSERT INTO loans (player_id, principal, remaining_balance, apr, term_days, issued_at, next_payment_due, missed_payments, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, 0, 'ACTIVE')";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, playerId.toString());
            statement.setDouble(2, loan.principal());
            statement.setDouble(3, loan.remainingBalance());
            statement.setDouble(4, loan.aprPercent());
            statement.setInt(5, loan.termDays());
            statement.setLong(6, loan.issuedAtMillis());
            statement.setLong(7, loan.nextPaymentDueMillis());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to insert loan for " + playerId, ex);
        }
        return -1;
    }

    private void update(UUID playerId, Loan loan) {
        String sql = "UPDATE loans SET remaining_balance = ?, next_payment_due = ?, missed_payments = ?, status = ? WHERE id = ?";
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setDouble(1, loan.remainingBalance());
                statement.setLong(2, loan.nextPaymentDueMillis());
                statement.setInt(3, loan.missedPayments());
                statement.setString(4, loan.status().name());
                statement.setLong(5, loan.id());
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to update loan #" + loan.id(), ex);
            }
        });
    }
}
