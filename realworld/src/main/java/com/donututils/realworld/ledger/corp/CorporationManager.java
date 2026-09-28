package com.donututils.realworld.ledger.corp;

import com.donututils.realworld.RealWorldPlugin;
import com.donututils.realworld.ledger.config.LedgerConfig;
import com.donututils.realworld.ledger.db.DatabaseManager;
import com.donututils.realworld.ledger.economy.LedgerEconomyProvider;
import com.donututils.realworld.ledger.model.Corporation;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Corporation lifecycle: founding, owner-reported financials, and treasury/trust management. Share
 * trading and price ticking live in {@link com.donututils.realworld.ledger.stock.ShareTradingManager} - this
 * class owns the corporation's identity and books, that one owns its market.
 * <p>
 * Thread contract: {@link #found} and {@link #reportFinancials} block synchronously on the database
 * (they need a definite success/failure and, for founding, a generated id) - call them from an async
 * task, never directly from a command handler.
 */
public final class CorporationManager {

    public record CorpResult(boolean success, String message, Corporation corporation) {
        static CorpResult fail(String message) {
            return new CorpResult(false, message, null);
        }

        static CorpResult ok(String message, Corporation corporation) {
            return new CorpResult(true, message, corporation);
        }
    }

    private final RealWorldPlugin plugin;
    private final DatabaseManager database;
    private final LedgerEconomyProvider economy;
    private final Supplier<LedgerConfig> configSupplier;

    private final Map<Long, Corporation> byId = new ConcurrentHashMap<>();
    private final Map<String, Long> idByTicker = new ConcurrentHashMap<>();
    private final Map<String, Long> idByName = new ConcurrentHashMap<>();
    // corpId -> playerId -> shares held
    private final Map<Long, Map<UUID, Integer>> shareholdings = new ConcurrentHashMap<>();

    public CorporationManager(RealWorldPlugin plugin, DatabaseManager database, LedgerEconomyProvider economy,
                               Supplier<LedgerConfig> configSupplier) {
        this.plugin = plugin;
        this.database = database;
        this.economy = economy;
        this.configSupplier = configSupplier;
        loadAll();
    }

    private void loadAll() {
        String corpSql = "SELECT id, name, ticker, founder_id, treasury_balance, total_shares, share_price, sector, founded_at, trust_protected FROM corporations";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(corpSql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                Corporation corp = new Corporation(
                        rows.getLong("id"), rows.getString("name"), rows.getString("ticker"),
                        UUID.fromString(rows.getString("founder_id")), rows.getInt("total_shares"),
                        rows.getDouble("share_price"), rows.getString("sector"), rows.getLong("founded_at"));
                corp.setTreasuryBalance(rows.getDouble("treasury_balance"));
                corp.setTrustProtected(rows.getInt("trust_protected") != 0);
                index(corp);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load corporations", ex);
        }

        String holdingsSql = "SELECT corporation_id, player_id, shares FROM shareholdings";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(holdingsSql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                long corpId = rows.getLong("corporation_id");
                UUID playerId = UUID.fromString(rows.getString("player_id"));
                shareholdings.computeIfAbsent(corpId, id -> new ConcurrentHashMap<>()).put(playerId, rows.getInt("shares"));
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load shareholdings", ex);
        }
    }

    private void index(Corporation corp) {
        byId.put(corp.id(), corp);
        idByTicker.put(corp.ticker().toUpperCase(Locale.ROOT), corp.id());
        idByName.put(corp.name().toLowerCase(Locale.ROOT), corp.id());
    }

    public Corporation getByTicker(String ticker) {
        Long id = idByTicker.get(ticker.toUpperCase(Locale.ROOT));
        return id == null ? null : byId.get(id);
    }

    public Corporation getById(long id) {
        return byId.get(id);
    }

    public Map<UUID, Integer> shareholdersOf(long corpId) {
        return shareholdings.getOrDefault(corpId, Map.of());
    }

    public int sharesHeldBy(long corpId, UUID playerId) {
        return shareholdings.getOrDefault(corpId, Map.of()).getOrDefault(playerId, 0);
    }

    public void setShares(long corpId, UUID playerId, int shares) {
        Map<UUID, Integer> holders = shareholdings.computeIfAbsent(corpId, id -> new ConcurrentHashMap<>());
        if (shares <= 0) {
            holders.remove(playerId);
        } else {
            holders.put(playerId, shares);
        }
    }

    public java.util.Collection<Corporation> all() {
        return byId.values();
    }

    public CorpResult found(OfflinePlayer founder, String name, String ticker, String sector) {
        if (name == null || name.isBlank() || ticker == null || ticker.isBlank()) {
            return CorpResult.fail("Name and ticker are required.");
        }
        String normalizedTicker = ticker.toUpperCase(Locale.ROOT);
        if (idByTicker.containsKey(normalizedTicker)) {
            return CorpResult.fail("A corporation with ticker " + normalizedTicker + " already exists.");
        }
        if (idByName.containsKey(name.toLowerCase(Locale.ROOT))) {
            return CorpResult.fail("A corporation named " + name + " already exists.");
        }

        LedgerConfig config = configSupplier.get();
        double cost = config.corpFoundingCost();
        EconomyResponse withdrawal = economy.withdrawPlayer(founder, cost);
        if (!withdrawal.transactionSuccess()) {
            return CorpResult.fail("You need " + economy.format(cost) + " to found a corporation.");
        }

        int totalShares = config.corpDefaultTotalShares();
        double startingSharePrice = cost / totalShares;
        Corporation corp = new Corporation(-1, name, normalizedTicker, founder.getUniqueId(), totalShares,
                startingSharePrice, sector == null || sector.isBlank() ? "General" : sector, System.currentTimeMillis());

        long id = insertCorporation(corp);
        if (id < 0) {
            economy.depositPlayer(founder, cost); // refund - the founding itself failed
            return CorpResult.fail("Something went wrong founding the corporation - try again.");
        }
        corp.setId(id);
        index(corp);
        setShares(id, founder.getUniqueId(), totalShares);
        insertShareholding(id, founder.getUniqueId(), totalShares);

        return CorpResult.ok(String.format(Locale.US, "Founded %s (%s) with %d shares at %s each.",
                name, normalizedTicker, totalShares, economy.format(startingSharePrice)), corp);
    }

    /** Owner-reported revenue/expenses for a reporting period. Nudges share price by the reported
     * profit/loss relative to market cap, and flows a matching, equally-clamped amount into the
     * corporation's treasury.
     * <p>
     * Both the price move AND the treasury credit are capped at
     * {@code corporations.max-report-impact-percent} of the corporation's current market cap - the
     * treasury is deliberately NOT credited with the raw reported profit, since that figure is
     * founder-supplied and unverified (nothing stops a founder from typing an arbitrary number).
     * Capping it to the same bounded fraction already used for the price nudge means one report can
     * only ever move real money by a small, size-relative amount, and {@code report-cooldown-minutes}
     * stops that from being repeated rapidly to compound into unlimited money. */
    public CorpResult reportFinancials(OfflinePlayer reporter, Corporation corp, double revenue, double expenses) {
        if (!corp.founderId().equals(reporter.getUniqueId())) {
            return CorpResult.fail("Only " + corp.name() + "'s founder can report its financials.");
        }
        if (revenue < 0 || expenses < 0) {
            return CorpResult.fail("Revenue and expenses can't be negative.");
        }

        LedgerConfig config = configSupplier.get();
        long cooldownMillis = config.corpReportCooldownMinutes() * 60_000L;
        long now = System.currentTimeMillis();
        long sinceLast = now - corp.lastReportedAtMillis();
        if (corp.lastReportedAtMillis() > 0 && sinceLast < cooldownMillis) {
            long remainingMinutes = (cooldownMillis - sinceLast) / 60_000L + 1;
            return CorpResult.fail(corp.name() + " can report again in " + remainingMinutes + " minute(s).");
        }

        double profit = revenue - expenses;
        double marketCap = Math.max(1.0, corp.marketCap());
        double maxImpactFraction = config.corpMaxReportImpactPercent() / 100.0;
        double rawImpact = (profit / marketCap) * config.corpProfitPriceSensitivity();
        double clampedImpact = Math.max(-maxImpactFraction, Math.min(maxImpactFraction, rawImpact));

        corp.setSharePrice(corp.sharePrice() * (1 + clampedImpact));
        corp.setTreasuryBalance(corp.treasuryBalance() + marketCap * clampedImpact);
        corp.setLastReportedAtMillis(now);

        insertFinancialReport(corp.id(), reporter.getUniqueId(), revenue, expenses);
        updateCorporation(corp);

        return CorpResult.ok(String.format(Locale.US,
                "Reported %s revenue / %s expenses (%s%s claimed profit). Treasury adjusted by %s (capped at %.0f%% of market cap). New share price: %s.",
                economy.format(revenue), economy.format(expenses),
                profit >= 0 ? "+" : "", economy.format(profit),
                economy.format(marketCap * clampedImpact), config.corpMaxReportImpactPercent(),
                economy.format(corp.sharePrice())), corp);
    }

    public CorpResult setTrust(OfflinePlayer founder, Corporation corp, boolean enabled) {
        if (!corp.founderId().equals(founder.getUniqueId())) {
            return CorpResult.fail("Only " + corp.name() + "'s founder can manage its trust status.");
        }
        if (enabled == corp.trustProtected()) {
            return CorpResult.fail("Trust protection is already " + (enabled ? "enabled" : "disabled") + ".");
        }
        LedgerConfig config = configSupplier.get();
        if (enabled) {
            EconomyResponse withdrawal = economy.withdrawPlayer(founder, config.trustSetupCost());
            if (!withdrawal.transactionSuccess()) {
                return CorpResult.fail("Setting up a trust costs " + economy.format(config.trustSetupCost()) + ".");
            }
        }
        corp.setTrustProtected(enabled);
        updateCorporation(corp);
        return CorpResult.ok("Trust protection " + (enabled ? "enabled" : "disabled") + " for " + corp.name() + ".", corp);
    }

    private long insertCorporation(Corporation corp) {
        String sql = "INSERT INTO corporations (name, ticker, founder_id, treasury_balance, total_shares, share_price, sector, founded_at, trust_protected) "
                + "VALUES (?, ?, ?, 0, ?, ?, ?, ?, 0)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, corp.name());
            statement.setString(2, corp.ticker());
            statement.setString(3, corp.founderId().toString());
            statement.setInt(4, corp.totalShares());
            statement.setDouble(5, corp.sharePrice());
            statement.setString(6, corp.sector());
            statement.setLong(7, corp.foundedAtMillis());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to insert corporation " + corp.name(), ex);
        }
        return -1;
    }

    public void updateCorporation(Corporation corp) {
        String sql = "UPDATE corporations SET treasury_balance = ?, share_price = ?, trust_protected = ? WHERE id = ?";
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setDouble(1, corp.treasuryBalance());
                statement.setDouble(2, corp.sharePrice());
                statement.setInt(3, corp.trustProtected() ? 1 : 0);
                statement.setLong(4, corp.id());
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to update corporation #" + corp.id(), ex);
            }
        });
    }

    public void upsertShareholding(long corpId, UUID playerId, int shares) {
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = database.getConnection()) {
                if (shares <= 0) {
                    try (PreparedStatement delete = connection.prepareStatement(
                            "DELETE FROM shareholdings WHERE corporation_id = ? AND player_id = ?")) {
                        delete.setLong(1, corpId);
                        delete.setString(2, playerId.toString());
                        delete.executeUpdate();
                    }
                    return;
                }
                String sql = "INSERT INTO shareholdings (corporation_id, player_id, shares) VALUES (?, ?, ?) "
                        + "ON CONFLICT(corporation_id, player_id) DO UPDATE SET shares = excluded.shares";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setLong(1, corpId);
                    statement.setString(2, playerId.toString());
                    statement.setInt(3, shares);
                    statement.executeUpdate();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to persist shareholding", ex);
            }
        });
    }

    private void insertShareholding(long corpId, UUID playerId, int shares) {
        upsertShareholding(corpId, playerId, shares);
    }

    private void insertFinancialReport(long corpId, UUID reporter, double revenue, double expenses) {
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO corp_financials (corporation_id, revenue, expenses, reported_by, reported_at) VALUES (?, ?, ?, ?, ?)";
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, corpId);
                statement.setDouble(2, revenue);
                statement.setDouble(3, expenses);
                statement.setString(4, reporter.toString());
                statement.setLong(5, System.currentTimeMillis());
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to record financial report for corp #" + corpId, ex);
            }
        });
    }
}
