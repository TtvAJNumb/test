package com.donututils.municipal.permit;

import com.donututils.municipal.config.MunicipalConfig;
import com.donututils.municipal.config.PermitDefinition;
import com.donututils.municipal.db.DatabaseManager;
import com.donututils.municipal.economy.VaultEconomyBridge;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Business/building/weapon permits: purchasable via Vault, granted as a real Bukkit permission node
 * via a {@link PermissionAttachment} (which does NOT persist across sessions - every owned permit is
 * re-applied on join) and persisted so ownership survives restarts.
 */
public final class PermitManager {

    private final Plugin plugin;
    private final DatabaseManager database;
    private final VaultEconomyBridge economy;
    private final Supplier<MunicipalConfig> configSupplier;

    private final Map<UUID, Set<String>> ownedPermits = new ConcurrentHashMap<>();
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();

    public PermitManager(Plugin plugin, DatabaseManager database, VaultEconomyBridge economy,
                          Supplier<MunicipalConfig> configSupplier) {
        this.plugin = plugin;
        this.database = database;
        this.economy = economy;
        this.configSupplier = configSupplier;
        loadAll();
    }

    private void loadAll() {
        String sql = "SELECT player_id, permit_type FROM permits";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                try {
                    UUID playerId = UUID.fromString(rows.getString("player_id"));
                    ownedPermits.computeIfAbsent(playerId, id -> ConcurrentHashMap.newKeySet()).add(rows.getString("permit_type"));
                } catch (IllegalArgumentException ignored) {
                    // skip malformed row
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load permits", ex);
        }
    }

    public boolean hasPermit(UUID playerId, String type) {
        return ownedPermits.getOrDefault(playerId, Set.of()).contains(type.toLowerCase(Locale.ROOT));
    }

    public Set<String> permitsOf(UUID playerId) {
        return ownedPermits.getOrDefault(playerId, Set.of());
    }

    public record PurchaseResult(boolean success, String message) {
    }

    public PurchaseResult buy(Player player, String type) {
        MunicipalConfig config = configSupplier.get();
        PermitDefinition definition = config.permit(type);
        if (definition == null) {
            return new PurchaseResult(false, "Unknown permit type: " + type);
        }
        String normalizedType = type.toLowerCase(Locale.ROOT);
        if (hasPermit(player.getUniqueId(), normalizedType)) {
            return new PurchaseResult(false, "You already have a " + normalizedType + " permit.");
        }
        VaultEconomyBridge.EconomyResult withdrawal = economy.withdraw(player.getUniqueId(), definition.cost());
        if (!withdrawal.success()) {
            return new PurchaseResult(false, "You need $" + String.format(Locale.US, "%,.2f", definition.cost()) + " for a " + normalizedType + " permit.");
        }
        ownedPermits.computeIfAbsent(player.getUniqueId(), id -> ConcurrentHashMap.newKeySet()).add(normalizedType);
        grant(player, definition.permission());
        persistAsync(player.getUniqueId(), normalizedType);
        return new PurchaseResult(true, "Purchased your " + normalizedType + " permit for $" + String.format(Locale.US, "%,.2f", definition.cost()) + ".");
    }

    /** Re-applies every permission attachment for permits this player already owns - call this on
     * join, since PermissionAttachments don't survive a relog. */
    public void reapplyOnJoin(Player player) {
        MunicipalConfig config = configSupplier.get();
        for (String type : permitsOf(player.getUniqueId())) {
            PermitDefinition definition = config.permit(type);
            if (definition != null) {
                grant(player, definition.permission());
            }
        }
    }

    private void grant(Player player, String permissionNode) {
        PermissionAttachment attachment = attachments.computeIfAbsent(player.getUniqueId(), id -> player.addAttachment(plugin));
        attachment.setPermission(permissionNode, true);
    }

    private void persistAsync(UUID playerId, String type) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO permits (player_id, permit_type, purchased_at) VALUES (?, ?, ?) "
                    + "ON CONFLICT(player_id, permit_type) DO NOTHING";
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerId.toString());
                statement.setString(2, type);
                statement.setLong(3, System.currentTimeMillis());
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to persist permit for " + playerId, ex);
            }
        });
    }
}
