package com.donututils.municipal;

import com.donututils.municipal.claim.ClaimManager;
import com.donututils.municipal.claim.ClaimProtectionListener;
import com.donututils.municipal.command.ClaimCommand;
import com.donututils.municipal.command.CourtCommand;
import com.donututils.municipal.command.PermitCommand;
import com.donututils.municipal.command.PoliceCommand;
import com.donututils.municipal.config.MunicipalConfig;
import com.donututils.municipal.config.PermitDefinition;
import com.donututils.municipal.court.CourtManager;
import com.donututils.municipal.db.DatabaseManager;
import com.donututils.municipal.economy.VaultEconomyBridge;
import com.donututils.municipal.jail.JailListener;
import com.donututils.municipal.jail.JailManager;
import com.donututils.municipal.location.LocationManager;
import com.donututils.municipal.command.LocationCommand;
import com.donututils.municipal.permit.PermitEnforcementListener;
import com.donututils.municipal.permit.PermitManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Municipal: law enforcement, courts, jail, and business/building/weapon permits. Standalone -
 * bridges to whatever Vault economy is registered (Ledger, or anything else) purely by reflection,
 * same pattern as this repo's other Vault-consuming plugins.
 */
public final class MunicipalPlugin extends JavaPlugin {

    private static final int MAX_VAULT_RETRIES = 10;

    private DatabaseManager databaseManager;
    private VaultEconomyBridge economy;
    private PermitManager permitManager;
    private CourtManager courtManager;
    private JailManager jailManager;
    private ClaimManager claimManager;
    private LocationManager locationManager;

    private volatile MunicipalConfig config;
    private BukkitTask releaseTask;
    private BukkitTask propertyTaxTask;
    private int vaultRetryAttempts;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = loadConfigValues();

        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault is not installed. Municipal needs Vault (plus any Vault-compatible economy, e.g. Ledger) for permit costs. Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        tryResolveVaultAndFinishEnable();
    }

    private void tryResolveVaultAndFinishEnable() {
        try {
            economy = VaultEconomyBridge.create();
        } catch (ReflectiveOperationException | RuntimeException ex) {
            getLogger().severe("Vault's economy API doesn't look like what this plugin expects: " + ex);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (economy == null) {
            vaultRetryAttempts++;
            if (vaultRetryAttempts > MAX_VAULT_RETRIES) {
                getLogger().severe("No economy plugin registered with Vault after waiting. Disabling.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            getServer().getScheduler().runTaskLater(this, this::tryResolveVaultAndFinishEnable, 20L);
            return;
        }

        finishEnable();
    }

    private void finishEnable() {
        databaseManager = new DatabaseManager(getDataFolder(), getLogger());
        locationManager = new LocationManager(this, databaseManager);
        permitManager = new PermitManager(this, databaseManager, economy, this::getMunicipalConfig);
        courtManager = new CourtManager(this, databaseManager);
        jailManager = new JailManager(this, databaseManager, this::getMunicipalConfig, locationManager);
        claimManager = new ClaimManager(this, databaseManager, this::getMunicipalConfig);

        getServer().getPluginManager().registerEvents(new PermitEnforcementListener(this::getMunicipalConfig), this);
        getServer().getPluginManager().registerEvents(new JailListener(jailManager, this::getMunicipalConfig), this);
        getServer().getPluginManager().registerEvents(new PermitJoinListener(), this);
        getServer().getPluginManager().registerEvents(new ClaimProtectionListener(claimManager), this);

        registerCommand("permit", new PermitCommand(permitManager));
        registerCommand("police", new PoliceCommand(this, courtManager));
        registerCommand("court", new CourtCommand(this, courtManager, jailManager, this::getMunicipalConfig));
        registerCommand("municipal", new ClaimCommand(this, claimManager, economy));
        registerCommand("location", new LocationCommand(locationManager));

        startTasks();

        getLogger().info("Municipal enabled - courts, jail, and permits are live.");
    }

    @Override
    public void onDisable() {
        stopTasks();
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }

    public void reloadMunicipal() {
        reloadConfig();
        config = loadConfigValues();
    }

    public MunicipalConfig getMunicipalConfig() {
        return config;
    }

    private void startTasks() {
        long releaseTicks = Math.max(20L, config.releaseCheckSeconds() * 20L);
        releaseTask = getServer().getScheduler().runTaskTimer(this, jailManager::tickReleases, releaseTicks, releaseTicks);

        long taxTicks = Math.max(20L, config.taxTickHours() * 3600L * 20L);
        propertyTaxTask = getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> claimManager.tickPropertyTax(economy), taxTicks, taxTicks);
    }

    private void stopTasks() {
        if (releaseTask != null) {
            releaseTask.cancel();
            releaseTask = null;
        }
        if (propertyTaxTask != null) {
            propertyTaxTask.cancel();
            propertyTaxTask = null;
        }
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        } else {
            getLogger().warning("plugin.yml is missing the '" + name + "' command definition.");
        }
    }

    private MunicipalConfig loadConfigValues() {
        FileConfiguration cfg = getConfig();

        Map<String, PermitDefinition> permits = new LinkedHashMap<>();
        for (String type : new String[]{"business", "building", "weapon"}) {
            double cost = cfg.getDouble("permits." + type + ".cost", 1000.0);
            String permission = cfg.getString("permits." + type + ".permission", "municipal.permit." + type);
            permits.put(type, new PermitDefinition(type, cost, permission));
        }

        Set<String> cityLimitWorlds = new HashSet<>(cfg.getStringList("enforcement.city-limit-worlds"));
        Set<String> controlledWeapons = new HashSet<>();
        for (String material : cfg.getStringList("enforcement.controlled-weapons")) {
            controlledWeapons.add(material.toUpperCase(java.util.Locale.ROOT));
        }
        Set<String> allowedCommands = new HashSet<>();
        for (String cmd : cfg.getStringList("jail.allowed-commands")) {
            allowedCommands.add(cmd.toLowerCase(java.util.Locale.ROOT));
        }
        allowedCommands.add("help");

        return new MunicipalConfig(
                permits,
                cityLimitWorlds,
                controlledWeapons,
                cfg.getString("jail.world", "world"),
                cfg.getDouble("jail.x", 0),
                cfg.getDouble("jail.y", 100),
                cfg.getDouble("jail.z", 0),
                cfg.getDouble("jail.radius", 10.0),
                allowedCommands,
                cfg.getInt("jail.release-check-seconds", 30),
                cfg.getInt("court.max-sentence-minutes", 10080),
                cfg.getDouble("claims.claim-fee", 500.0),
                cfg.getDouble("claims.tax-per-chunk", 50.0),
                cfg.getInt("claims.tax-tick-hours", 24),
                cfg.getInt("claims.foreclosure-after-missed-ticks", 14)
        );
    }

    /** Re-applies permission attachments for a player's already-owned permits on join, since
     * PermissionAttachments don't survive a relog. */
    private final class PermitJoinListener implements org.bukkit.event.Listener {
        @org.bukkit.event.EventHandler
        public void onJoin(PlayerJoinEvent event) {
            permitManager.reapplyOnJoin(event.getPlayer());
        }
    }
}
