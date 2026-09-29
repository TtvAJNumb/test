package com.donututils.donutrep.economy;

import com.donututils.donutrep.economy.db.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Shards: the premium/milestone currency (earned from legacy resets, rare events, or webstore
 * redemptions - never from ordinary gameplay income). A deliberately simple in-memory-cached,
 * write-behind-persisted integer balance per player, same pattern as Ledger's own checking balance
 * cache in {@link com.donututils.donutrep.economy.EconomyManager} - not a Vault
 * currency, since Vault's Economy interface is money-shaped (doubles, bank accounts) and Shards are a
 * simpler whole-number milestone counter that only Market and admin commands ever touch.
 */
public final class ShardManager {

    private final Plugin plugin;
    private final DatabaseManager database;
    private final Map<UUID, Long> balances = new ConcurrentHashMap<>();

    public ShardManager(Plugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        loadAll();
    }

    private void loadAll() {
        String sql = "SELECT player_id, balance FROM shard_balances";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                try {
                    balances.put(UUID.fromString(rows.getString("player_id")), rows.getLong("balance"));
                } catch (IllegalArgumentException ignored) {
                    // skip malformed row
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load Shard balances", ex);
        }
    }

    public long balance(UUID playerId) {
        return balances.getOrDefault(playerId, 0L);
    }

    /** Snapshot of every player this plugin currently knows a Shards balance for - used by
     * TeamLeaderboard's shards ranking. */
    public Map<UUID, Long> allBalances() {
        return Map.copyOf(balances);
    }

    public void credit(UUID playerId, long amount) {
        if (amount <= 0) {
            return;
        }
        long updated = balances.merge(playerId, amount, Long::sum);
        persistAsync(playerId, updated);
    }

    /** Returns false (no-op) if the player doesn't have enough Shards. */
    public boolean debit(UUID playerId, long amount) {
        if (amount <= 0) {
            return true;
        }
        synchronized (this) {
            long current = balance(playerId);
            if (current < amount) {
                return false;
            }
            long updated = current - amount;
            balances.put(playerId, updated);
            persistAsync(playerId, updated);
            return true;
        }
    }

    private void persistAsync(UUID playerId, long balance) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO shard_balances (player_id, balance) VALUES (?, ?) "
                    + "ON CONFLICT(player_id) DO UPDATE SET balance = excluded.balance";
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerId.toString());
                statement.setLong(2, balance);
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to persist Shard balance for " + playerId, ex);
            }
        });
    }
}
