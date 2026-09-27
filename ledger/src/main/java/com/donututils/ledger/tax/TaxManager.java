package com.donututils.ledger.tax;

import com.donututils.ledger.LedgerPlugin;
import com.donututils.ledger.bank.BankManager;
import com.donututils.ledger.config.LedgerConfig;
import com.donututils.ledger.corp.CorporationManager;
import com.donututils.ledger.economy.LedgerEconomyProvider;
import com.donututils.ledger.model.Corporation;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Two periodic sweeps (wealth tax on players, corporate tax on treasuries) plus a reusable
 * per-transaction tax helper that command handlers call after a successful trade/transfer.
 * <p>
 * Write-offs: a corporation's treasury already reflects revenue net of reported expenses (see
 * {@link CorporationManager#reportFinancials}), so corporate tax is naturally computed on
 * post-expense profit - there's no separate "undo the write-off" step. Trust protection shields a
 * configurable percentage of a trust-flagged corporation's treasury from corporate tax entirely.
 */
public final class TaxManager {

    private final LedgerPlugin plugin;
    private final LedgerEconomyProvider economy;
    private final BankManager bankManager;
    private final CorporationManager corporationManager;
    private final Supplier<LedgerConfig> configSupplier;

    public TaxManager(LedgerPlugin plugin, LedgerEconomyProvider economy, BankManager bankManager,
                       CorporationManager corporationManager, Supplier<LedgerConfig> configSupplier) {
        this.plugin = plugin;
        this.economy = economy;
        this.bankManager = bankManager;
        this.corporationManager = corporationManager;
        this.configSupplier = configSupplier;
    }

    /** Taxes each known player's checking + savings balance. Only what's actually available in
     * checking is collected - this is a game mechanic, not a real tax authority, so it never seizes
     * savings or goes negative. */
    public void collectWealthTax() {
        double ratePercent = configSupplier.get().wealthTaxPercent();
        if (ratePercent <= 0) {
            return;
        }
        Set<UUID> knownPlayers = new HashSet<>(economy.allCheckingBalances().keySet());
        knownPlayers.addAll(bankManager.allSavingsBalances().keySet());

        for (UUID playerId : knownPlayers) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
            double totalWealth = economy.getBalance(player) + bankManager.getSavingsBalance(playerId);
            double tax = totalWealth * ratePercent / 100.0;
            if (tax <= 0) {
                continue;
            }
            double collectible = Math.min(tax, economy.getBalance(player));
            if (collectible <= 0) {
                continue;
            }
            economy.withdrawPlayer(player, collectible);
            recordTax(playerId, null, collectible, "WEALTH");
        }
    }

    /** Taxes each corporation's treasury, applying trust-shield protection where enabled. */
    public void collectCorporateTax() {
        double ratePercent = configSupplier.get().corporateTaxPercent();
        if (ratePercent <= 0) {
            return;
        }
        double trustShieldPercent = configSupplier.get().trustShieldPercent();
        for (Corporation corp : corporationManager.all()) {
            double taxableBase = Math.max(0, corp.treasuryBalance());
            if (corp.trustProtected()) {
                taxableBase *= (1 - trustShieldPercent / 100.0);
            }
            double tax = taxableBase * ratePercent / 100.0;
            if (tax <= 0) {
                continue;
            }
            corp.setTreasuryBalance(corp.treasuryBalance() - tax);
            corporationManager.updateCorporation(corp);
            recordTax(null, corp.id(), tax, "CORPORATE");
        }
    }

    /** Called by command handlers right after a successful trade/transfer that should be taxed.
     * Withdraws the tax from the payer's checking balance (best-effort - if they can't cover it, no
     * tax is collected rather than blocking the underlying transaction that already happened) and
     * returns the amount actually collected. */
    public double applyTransactionTax(OfflinePlayer payer, double grossAmount) {
        double ratePercent = configSupplier.get().transactionTaxPercent();
        if (ratePercent <= 0 || grossAmount <= 0) {
            return 0;
        }
        double tax = grossAmount * ratePercent / 100.0;
        EconomyResponse response = economy.withdrawPlayer(payer, tax);
        if (!response.transactionSuccess()) {
            return 0;
        }
        recordTax(payer.getUniqueId(), null, tax, "TRANSACTION");
        return tax;
    }

    private void recordTax(UUID playerId, Long corporationId, double amount, String type) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO tax_records (player_id, corporation_id, amount, tax_type, period) VALUES (?, ?, ?, ?, ?)";
            try (Connection connection = plugin.getDatabaseManager().getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerId == null ? null : playerId.toString());
                if (corporationId == null) {
                    statement.setNull(2, java.sql.Types.INTEGER);
                } else {
                    statement.setLong(2, corporationId);
                }
                statement.setDouble(3, amount);
                statement.setString(4, type);
                statement.setLong(5, System.currentTimeMillis());
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to record tax collection", ex);
            }
        });
    }
}
