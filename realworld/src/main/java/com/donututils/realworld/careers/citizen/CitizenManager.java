package com.donututils.realworld.careers.citizen;

import com.donututils.realworld.careers.config.AgeTier;
import com.donututils.realworld.careers.config.CareersConfig;
import com.donututils.realworld.careers.config.JobDefinition;
import com.donututils.realworld.careers.db.DatabaseManager;
import com.donututils.realworld.careers.economy.VaultEconomyBridge;
import com.donututils.realworld.careers.model.CitizenProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;

/** Owns every player's age tier / job / wage-timing, cached in memory and backed by SQLite. */
public final class CitizenManager {

    private final Plugin plugin;
    private final DatabaseManager database;
    private final Supplier<CareersConfig> configSupplier;
    private final Map<UUID, CitizenProfile> profiles = new ConcurrentHashMap<>();

    public CitizenManager(Plugin plugin, DatabaseManager database, Supplier<CareersConfig> configSupplier) {
        this.plugin = plugin;
        this.database = database;
        this.configSupplier = configSupplier;
        loadAll();
    }

    private void loadAll() {
        String sql = "SELECT player_id, age_tier, job_id, last_wage_at, playtime_minutes, legacy_count FROM citizens";
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                UUID playerId = UUID.fromString(rows.getString("player_id"));
                String tierRaw = rows.getString("age_tier");
                AgeTier tier = tierRaw == null ? null : AgeTier.valueOf(tierRaw);
                CitizenProfile profile = new CitizenProfile(playerId, tier, rows.getString("job_id"), rows.getLong("last_wage_at"),
                        rows.getInt("playtime_minutes"), rows.getInt("legacy_count"));
                profiles.put(playerId, profile);
            }
        } catch (SQLException | IllegalArgumentException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load citizens", ex);
        }
        plugin.getLogger().info("Loaded " + profiles.size() + " citizen profile(s).");
    }

    /** Never null - creates and persists a fresh, unset profile on first lookup. */
    public CitizenProfile profileOf(UUID playerId) {
        return profiles.computeIfAbsent(playerId, id -> {
            CitizenProfile profile = new CitizenProfile(id, null, null, System.currentTimeMillis());
            persistAsync(profile);
            return profile;
        });
    }

    public void setAgeTier(UUID playerId, AgeTier tier) {
        CitizenProfile profile = profileOf(playerId);
        profile.setAgeTier(tier);
        persistAsync(profile);
    }

    public void setJob(UUID playerId, String jobId) {
        CitizenProfile profile = profileOf(playerId);
        profile.setJobId(jobId);
        profile.setLastWageAtMillis(System.currentTimeMillis());
        persistAsync(profile);
    }

    /** Scheduled on an async repeating task. Pays every citizen with a chosen job whose wage
     * interval has elapsed, scaled up by their permanent legacy wage bonus (if any - see
     * {@link #performLegacyReset}). */
    public void tickWages(VaultEconomyBridge economy) {
        CareersConfig config = configSupplier.get();
        long now = System.currentTimeMillis();
        for (CitizenProfile profile : profiles.values()) {
            if (!profile.hasChosenJob()) {
                continue;
            }
            JobDefinition job = config.job(profile.jobId());
            if (job == null) {
                continue;
            }
            long intervalMillis = job.wageIntervalMinutes() * 60_000L;
            if (now - profile.lastWageAtMillis() < intervalMillis) {
                continue;
            }
            double wage = job.wageAmount() * wageBonusMultiplier(profile, config);
            economy.deposit(profile.playerId(), wage);
            profile.setLastWageAtMillis(now);
            persistAsync(profile);
            notifyIfOnline(profile.playerId(), "&aPaid " + formatMoney(wage) + " wage as a " + stripColor(job.displayName()) + ".");
        }
    }

    /** Multiplier applied to every wage/task payout from a citizen's permanent legacy count - a
     * small, ever-growing, real veteran-player perk (never resets, unlike everything else a legacy
     * reset wipes). 1.0 for a player who has never gone through a legacy reset. */
    public double wageBonusMultiplier(CitizenProfile profile, CareersConfig config) {
        return 1.0 + (profile.legacyCount() * config.legacyWageBonusPercentPerLegacy() / 100.0);
    }

    /** Called once a minute (see the scheduled task in {@link com.donututils.realworld.RealWorldPlugin})
     * for every online player: accrues tracked playtime and auto-graduates a Minor to Adult once the
     * configured threshold is reached - no admin command needed, this is the "school system". */
    public void tickPlaytime(java.util.Collection<Player> onlinePlayers) {
        CareersConfig config = configSupplier.get();
        for (Player player : onlinePlayers) {
            CitizenProfile profile = profileOf(player.getUniqueId());
            profile.setPlaytimeMinutes(profile.playtimeMinutes() + 1);
            if (profile.ageTier() == AgeTier.MINOR && profile.playtimeMinutes() >= config.minorToAdultPlaytimeMinutes()) {
                profile.setAgeTier(AgeTier.ADULT);
                player.sendMessage(color("&6&lYou graduated! &7You're now an Adult - every job is open to you. Use &f/career reopen &7to pick one."));
            }
            persistAsync(profile);
        }
    }

    /** Adults only. Resets age tier and job back to a fresh start, but keeps
     * legacy-inheritance-percent of the player's current wealth (Money + stock portfolio value,
     * passed in already computed since StockMarket lives in a different subsystem) as a lump-sum
     * inheritance, plus a Shard bonus, and permanently bumps their legacy count (and therefore
     * every future wage - see {@link #wageBonusMultiplier}). Returns null if the player isn't
     * currently an Adult. */
    public LegacyResult performLegacyReset(UUID playerId, double currentWealth) {
        CitizenProfile profile = profileOf(playerId);
        if (profile.ageTier() != AgeTier.ADULT) {
            return null;
        }
        CareersConfig config = configSupplier.get();
        double inheritance = currentWealth * (config.legacyInheritancePercent() / 100.0);
        int newLegacyCount = profile.legacyCount() + 1;

        profile.setAgeTier(AgeTier.MINOR);
        profile.setJobId(null);
        profile.setLegacyCount(newLegacyCount);
        profile.setPlaytimeMinutes(0);
        persistAsync(profile);

        return new LegacyResult(inheritance, newLegacyCount, config.legacyShardBonusPerLegacy());
    }

    public record LegacyResult(double inheritanceMoney, int newLegacyCount, long shardBonus) {
    }

    /** Called by task-bonus listeners (block break, fishing, breeding) - pays a bonus if the
     * player's current job earns one for that action, silently no-ops otherwise. */
    public void payBonusIfEligible(Player player, String jobIdRequired, double amount, String reason, VaultEconomyBridge economy) {
        if (amount <= 0) {
            return;
        }
        CitizenProfile profile = profileOf(player.getUniqueId());
        if (!jobIdRequired.equalsIgnoreCase(profile.jobId() == null ? "" : profile.jobId())) {
            return;
        }
        economy.deposit(player.getUniqueId(), amount);
        player.sendMessage(color("&a+" + formatMoney(amount) + " &7(" + reason + ")"));
    }

    private void persistAsync(CitizenProfile profile) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO citizens (player_id, age_tier, job_id, last_wage_at, playtime_minutes, legacy_count) VALUES (?, ?, ?, ?, ?, ?) "
                    + "ON CONFLICT(player_id) DO UPDATE SET age_tier = excluded.age_tier, job_id = excluded.job_id, "
                    + "last_wage_at = excluded.last_wage_at, playtime_minutes = excluded.playtime_minutes, legacy_count = excluded.legacy_count";
            try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, profile.playerId().toString());
                statement.setString(2, profile.ageTier() == null ? null : profile.ageTier().name());
                statement.setString(3, profile.jobId());
                statement.setLong(4, profile.lastWageAtMillis());
                statement.setInt(5, profile.playtimeMinutes());
                statement.setInt(6, profile.legacyCount());
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to persist citizen " + profile.playerId(), ex);
            }
        });
    }

    private void notifyIfOnline(UUID playerId, String message) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.sendMessage(color(message));
        }
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static String stripColor(String message) {
        return ChatColor.stripColor(color(message));
    }

    private static String formatMoney(double amount) {
        return "$" + String.format(java.util.Locale.US, "%,.2f", amount);
    }
}
