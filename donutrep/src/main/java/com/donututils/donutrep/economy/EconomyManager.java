package com.donututils.donutrep.economy;

import com.donututils.donutrep.economy.db.DatabaseManager;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * DonutREP's own internal Money balance manager, registered directly with Vault's ServicesManager
 * as a real {@link Economy} provider - no external economy plugin involved. Deliberately just a flat
 * per-player balance (no savings/interest, credit, loans, or corporations - those were this plugin's
 * old Ledger subsystem, removed in favor of this simpler internal manager).
 * <p>
 * Vault's Economy interface is fully synchronous: callers expect getBalance()/depositPlayer() to
 * return immediately. To honor that without blocking the main thread on SQLite I/O for every call,
 * balances live in an in-memory cache (loaded once at startup) and every mutation writes through to
 * the cache immediately, with the actual database row updated on a background thread right after.
 */
public final class EconomyManager implements Economy {

    private final Plugin plugin;
    private final DatabaseManager database;
    private final double startingBalance;
    private final Map<UUID, Double> balances = new ConcurrentHashMap<>();

    public EconomyManager(Plugin plugin, DatabaseManager database, double startingBalance) {
        this.plugin = plugin;
        this.database = database;
        this.startingBalance = startingBalance;
        loadAllBalances();
    }

    private void loadAllBalances() {
        String sql = "SELECT player_id, balance FROM balances";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                try {
                    balances.put(UUID.fromString(rows.getString("player_id")), rows.getDouble("balance"));
                } catch (IllegalArgumentException ignored) {
                    // skip malformed row
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load Money balances from the database", ex);
        }
    }

    /** Ensures a player has a balance row, creating one with the configured starting balance if
     * not. Safe to call every join. */
    public void ensureAccount(UUID playerId) {
        balances.computeIfAbsent(playerId, id -> {
            persistBalanceAsync(id, startingBalance);
            return startingBalance;
        });
    }

    /** Snapshot of every player this plugin currently knows a Money balance for - used by
     * EconomyWatchdog's balance-jump sweep and MarketWatch's circulating-Money total. */
    public Map<UUID, Double> allBalances() {
        return Map.copyOf(balances);
    }

    private void persistBalanceAsync(UUID playerId, double balance) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO balances (player_id, balance) VALUES (?, ?) "
                    + "ON CONFLICT(player_id) DO UPDATE SET balance = excluded.balance";
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerId.toString());
                statement.setDouble(2, balance);
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to persist Money balance for " + playerId, ex);
            }
        });
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "DonutREP";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double amount) {
        return String.format(Locale.US, "$%,.2f", amount);
    }

    @Override
    public String currencyNamePlural() {
        return "Dollars";
    }

    @Override
    public String currencyNameSingular() {
        return "Dollar";
    }

    @Override
    public boolean hasAccount(String playerName) {
        return hasAccount(offline(playerName));
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return balances.containsKey(player.getUniqueId());
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    public double getBalance(String playerName) {
        return getBalance(offline(playerName));
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return balances.getOrDefault(player.getUniqueId(), 0.0);
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return has(offline(playerName), amount);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return withdrawPlayer(offline(playerName), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Cannot withdraw a negative amount.");
        }
        UUID playerId = player.getUniqueId();
        ensureAccount(playerId);
        double current = balances.get(playerId);
        if (current < amount) {
            return new EconomyResponse(0, current, EconomyResponse.ResponseType.FAILURE, "Insufficient funds.");
        }
        double updated = current - amount;
        balances.put(playerId, updated);
        persistBalanceAsync(playerId, updated);
        return new EconomyResponse(amount, updated, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return depositPlayer(offline(playerName), amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Cannot deposit a negative amount.");
        }
        UUID playerId = player.getUniqueId();
        ensureAccount(playerId);
        double updated = balances.get(playerId) + amount;
        balances.put(playerId, updated);
        persistBalanceAsync(playerId, updated);
        return new EconomyResponse(amount, updated, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return notImplemented();
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return notImplemented();
    }

    @Override
    public List<String> getBanks() {
        return Collections.emptyList();
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return createPlayerAccount(offline(playerName));
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        ensureAccount(player.getUniqueId());
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }

    private static EconomyResponse notImplemented() {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "DonutREP does not support Vault's shared bank accounts.");
    }

    @SuppressWarnings("deprecation")
    private static OfflinePlayer offline(String playerName) {
        return Bukkit.getOfflinePlayer(playerName);
    }
}
