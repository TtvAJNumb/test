package com.donututils.motors;

import com.donututils.motors.command.MotorsCommand;
import com.donututils.motors.config.MotorsConfig;
import com.donututils.motors.config.VehicleDefinition;
import com.donututils.motors.db.DatabaseManager;
import com.donututils.motors.economy.VaultEconomyBridge;
import com.donututils.motors.vehicle.PlaceListener;
import com.donututils.motors.vehicle.VehicleItemFactory;
import com.donututils.motors.vehicle.VehicleKeys;
import com.donututils.motors.vehicle.VehicleManager;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Motors: custom drivable vehicles built entirely on vanilla Boat/Minecart riding physics - no
 * InfiniteVehicles dependency. Steering/acceleration/collision are 100% vanilla; Motors only meters
 * fuel, accrues wear from distance traveled, and decorates each vehicle with a resource-pack body.
 */
public final class MotorsPlugin extends JavaPlugin {

    private static final int MAX_VAULT_RETRIES = 10;

    private DatabaseManager databaseManager;
    private VaultEconomyBridge economy;
    private VehicleManager vehicleManager;

    private volatile MotorsConfig config;
    private BukkitTask tickTask;
    private int vaultRetryAttempts;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = loadConfigValues();

        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault is not installed. Motors needs Vault (plus any Vault-compatible economy, e.g. Ledger) for refuel/repair costs. Disabling.");
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

        VehicleKeys keys = new VehicleKeys(this);
        VehicleItemFactory itemFactory = new VehicleItemFactory(keys);
        vehicleManager = new VehicleManager(this, databaseManager, keys, itemFactory, this::getMotorsConfig);
        vehicleManager.loadFromDatabase();

        getServer().getPluginManager().registerEvents(new PlaceListener(vehicleManager, keys, this::getMotorsConfig), this);

        registerCommand("motors", new MotorsCommand(this, vehicleManager, itemFactory));

        tickTask = getServer().getScheduler().runTaskTimer(this, vehicleManager::tick, 20L, 20L);

        getLogger().info("Motors enabled with " + config.vehicles().size() + " vehicle(s) configured.");
    }

    @Override
    public void onDisable() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (vehicleManager != null) {
            vehicleManager.shutdownFlush();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }

    public void reloadMotors() {
        reloadConfig();
        config = loadConfigValues();
    }

    public MotorsConfig getMotorsConfig() {
        return config;
    }

    public VaultEconomyBridge getEconomy() {
        return economy;
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        } else {
            getLogger().warning("plugin.yml is missing the '" + name + "' command definition.");
        }
    }

    private MotorsConfig loadConfigValues() {
        FileConfiguration cfg = getConfig();
        Map<String, VehicleDefinition> vehicles = new LinkedHashMap<>();
        ConfigurationSection vehiclesSection = cfg.getConfigurationSection("vehicles");
        if (vehiclesSection != null) {
            for (String id : vehiclesSection.getKeys(false)) {
                ConfigurationSection s = vehiclesSection.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                try {
                    VehicleDefinition.Kind kind = VehicleDefinition.Kind.valueOf(
                            s.getString("kind", "BOAT").toUpperCase(Locale.ROOT));
                    Material keyMaterial = Material.valueOf(s.getString("key-material", "OAK_BOAT").toUpperCase(Locale.ROOT));
                    Material bodyHelmetMaterial = Material.valueOf(
                            s.getString("body-helmet-material", "LEATHER_HORSE_ARMOR").toUpperCase(Locale.ROOT));

                    VehicleDefinition definition = new VehicleDefinition(
                            id,
                            kind,
                            keyMaterial,
                            s.getInt("key-custom-model-data", 0),
                            s.getString("key-display-name", id),
                            bodyHelmetMaterial,
                            s.getInt("body-helmet-custom-model-data", 0),
                            s.getString("display-name", id),
                            s.getDouble("max-fuel", 1000.0),
                            s.getDouble("fuel-drain-per-second", 0.5),
                            s.getDouble("max-wear", 100.0),
                            s.getDouble("wear-per-block", 0.02),
                            s.getDouble("refuel-cost-per-unit", 0.5),
                            s.getDouble("repair-cost-per-wear", 4.0)
                    );
                    vehicles.put(id.toLowerCase(Locale.ROOT), definition);
                } catch (IllegalArgumentException ex) {
                    getLogger().warning("Skipping vehicle '" + id + "' - invalid config: " + ex.getMessage());
                }
            }
        }
        return new MotorsConfig(vehicles);
    }
}
