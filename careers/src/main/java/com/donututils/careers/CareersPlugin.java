package com.donututils.careers;

import com.donututils.careers.citizen.CitizenManager;
import com.donututils.careers.command.CareerCommand;
import com.donututils.careers.config.AgeTier;
import com.donututils.careers.config.CareersConfig;
import com.donututils.careers.config.JobDefinition;
import com.donututils.careers.db.DatabaseManager;
import com.donututils.careers.economy.VaultEconomyBridge;
import com.donututils.careers.job.TaskBonusListener;
import com.donututils.careers.onboarding.OnboardingGuiService;
import com.donututils.careers.onboarding.OnboardingListener;
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
 * Careers: on-join age tier + job selection, automated wages, and real task bonuses for jobs that
 * map to vanilla actions. Standalone - bridges to whatever Vault economy is registered (Ledger, or
 * anything else) purely by reflection, same pattern as this repo's other Vault-consuming plugins.
 */
public final class CareersPlugin extends JavaPlugin {

    private static final int MAX_VAULT_RETRIES = 10;

    private DatabaseManager databaseManager;
    private VaultEconomyBridge economy;
    private CitizenManager citizenManager;

    private volatile CareersConfig config;
    private BukkitTask wageTask;
    private int vaultRetryAttempts;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = loadConfigValues();

        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault is not installed. Careers needs Vault (plus any Vault-compatible economy, e.g. Ledger) to pay wages. Disabling.");
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
        citizenManager = new CitizenManager(this, databaseManager, this::getCareersConfig);
        OnboardingGuiService guiService = new OnboardingGuiService(citizenManager, this::getCareersConfig);

        getServer().getPluginManager().registerEvents(new OnboardingListener(this, citizenManager, guiService, this::getCareersConfig), this);
        getServer().getPluginManager().registerEvents(new TaskBonusListener(citizenManager, this::getCareersConfig, economy), this);

        registerCommand("career", new CareerCommand(this, citizenManager, guiService));

        wageTask = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> citizenManager.tickWages(economy), 20L * 60L, 20L * 60L);

        getLogger().info("Careers enabled with " + config.jobs().size() + " job(s) configured.");
    }

    @Override
    public void onDisable() {
        if (wageTask != null) {
            wageTask.cancel();
            wageTask = null;
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }

    public void reloadCareers() {
        reloadConfig();
        config = loadConfigValues();
    }

    public CareersConfig getCareersConfig() {
        return config;
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        } else {
            getLogger().warning("plugin.yml is missing the '" + name + "' command definition.");
        }
    }

    private CareersConfig loadConfigValues() {
        FileConfiguration cfg = getConfig();
        Map<String, JobDefinition> jobs = new LinkedHashMap<>();
        ConfigurationSection jobsSection = cfg.getConfigurationSection("jobs");
        if (jobsSection != null) {
            for (String id : jobsSection.getKeys(false)) {
                ConfigurationSection s = jobsSection.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                try {
                    AgeTier minAgeTier = AgeTier.valueOf(s.getString("min-age-tier", "ADULT").toUpperCase(Locale.ROOT));
                    Map<String, Double> blockBonuses = new LinkedHashMap<>();
                    ConfigurationSection bonusSection = s.getConfigurationSection("block-bonuses");
                    if (bonusSection != null) {
                        for (String material : bonusSection.getKeys(false)) {
                            blockBonuses.put(material.toUpperCase(Locale.ROOT), bonusSection.getDouble(material, 0.0));
                        }
                    }
                    JobDefinition job = new JobDefinition(
                            id,
                            s.getString("display-name", id),
                            minAgeTier,
                            s.getDouble("wage-amount", 25.0),
                            s.getInt("wage-interval-minutes", 30),
                            blockBonuses,
                            s.getDouble("fish-catch-bonus", 0.0),
                            s.getDouble("breed-bonus", 0.0)
                    );
                    jobs.put(id.toLowerCase(Locale.ROOT), job);
                } catch (IllegalArgumentException ex) {
                    getLogger().warning("Skipping job '" + id + "' - invalid config: " + ex.getMessage());
                }
            }
        }
        return new CareersConfig(jobs, cfg.getString("onboarding.welcome-message", "&6Welcome!"));
    }
}
