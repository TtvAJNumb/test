package com.donututils.realworld.ledger.credit;

import com.donututils.realworld.RealWorldPlugin;
import com.donututils.realworld.ledger.config.LedgerConfig;
import com.donututils.realworld.ledger.db.DatabaseManager;
import com.donututils.realworld.ledger.model.CreditProfile;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Tracks each player's credit score (300-850) and payment history. Everything here runs off the main
 * thread except reading the in-memory cache, which every other manager treats as the source of truth
 * for "what's this player's score right now" - loaded lazily and refreshed on every mutation.
 */
public final class CreditScoreManager {

    private final RealWorldPlugin plugin;
    private final DatabaseManager database;
    private final Supplier<LedgerConfig> configSupplier;
    private final Map<UUID, CreditProfile> cache = new ConcurrentHashMap<>();

    public CreditScoreManager(RealWorldPlugin plugin, DatabaseManager database, Supplier<LedgerConfig> configSupplier) {
        this.plugin = plugin;
        this.database = database;
        this.configSupplier = configSupplier;
    }

    /** Non-blocking: returns the cached profile, or a fresh default while a load happens in the
     * background if this is the first time this player's been touched this session. */
    public CreditProfile getCached(UUID playerId) {
        CreditProfile cached = cache.get(playerId);
        if (cached != null) {
            return cached;
        }
        ensureLoaded(playerId);
        return cache.getOrDefault(playerId, new CreditProfile(configSupplier.get().creditStartingScore(), 0, 0, 0));
    }

    public void ensureLoaded(UUID playerId) {
        if (cache.containsKey(playerId)) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            CreditProfile loaded = loadOrCreate(playerId);
            cache.putIfAbsent(playerId, loaded);
        });
    }

    private CreditProfile loadOrCreate(UUID playerId) {
        String select = "SELECT score, on_time_payments, missed_payments, defaults FROM credit_profiles WHERE player_id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(select)) {
            statement.setString(1, playerId.toString());
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) {
                    return new CreditProfile(rows.getInt("score"), rows.getInt("on_time_payments"),
                            rows.getInt("missed_payments"), rows.getInt("defaults"));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to load credit profile for " + playerId, ex);
        }
        CreditProfile fresh = new CreditProfile(configSupplier.get().creditStartingScore(), 0, 0, 0);
        persist(playerId, fresh);
        return fresh;
    }

    public void recordOnTimePayment(UUID playerId) {
        CreditProfile profile = getCached(playerId);
        profile.recordOnTimePayment(configSupplier.get().pointsPerOnTimePayment());
        persistAsync(playerId, profile);
    }

    public void recordMissedPayment(UUID playerId) {
        CreditProfile profile = getCached(playerId);
        profile.recordMissedPayment(configSupplier.get().pointsPerMissedPayment());
        persistAsync(playerId, profile);
    }

    public void recordDefault(UUID playerId) {
        CreditProfile profile = getCached(playerId);
        profile.recordDefault(configSupplier.get().pointsPerDefault());
        persistAsync(playerId, profile);
    }

    private void persistAsync(UUID playerId, CreditProfile profile) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> persist(playerId, profile));
    }

    private void persist(UUID playerId, CreditProfile profile) {
        String sql = "INSERT INTO credit_profiles (player_id, score, on_time_payments, missed_payments, defaults, last_updated) "
                + "VALUES (?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(player_id) DO UPDATE SET score = excluded.score, on_time_payments = excluded.on_time_payments, "
                + "missed_payments = excluded.missed_payments, defaults = excluded.defaults, last_updated = excluded.last_updated";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setInt(2, profile.score());
            statement.setInt(3, profile.onTimePayments());
            statement.setInt(4, profile.missedPayments());
            statement.setInt(5, profile.defaults());
            statement.setLong(6, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to persist credit profile for " + playerId, ex);
        }
    }
}
