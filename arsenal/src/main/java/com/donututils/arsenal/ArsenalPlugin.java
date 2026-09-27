package com.donututils.arsenal;

import com.donututils.arsenal.command.ArsenalCommand;
import com.donututils.arsenal.config.ArsenalConfig;
import com.donututils.arsenal.config.WeaponDefinition;
import com.donututils.arsenal.weapon.FireListener;
import com.donututils.arsenal.weapon.ReloadListener;
import com.donututils.arsenal.weapon.WeaponItemFactory;
import com.donututils.arsenal.weapon.WeaponKeys;
import com.donututils.arsenal.weapon.WeaponManager;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Arsenal: custom raycast-based firearms built entirely from vanilla Bukkit/Paper APIs - no
 * WeaponMechanics dependency. Weapons are ordinary items wearing a custom resource-pack model via
 * CustomModelData; see the delivered resource-pack-pipeline.md for how to build the actual assets.
 */
public final class ArsenalPlugin extends JavaPlugin {

    private WeaponKeys keys;
    private WeaponItemFactory itemFactory;
    private WeaponManager weaponManager;
    private volatile ArsenalConfig config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = loadConfigValues();

        keys = new WeaponKeys(this);
        itemFactory = new WeaponItemFactory(keys);
        weaponManager = new WeaponManager(this, keys, itemFactory, this::getArsenalConfig);

        getServer().getPluginManager().registerEvents(new FireListener(weaponManager), this);
        getServer().getPluginManager().registerEvents(new ReloadListener(weaponManager), this);

        registerCommand("arsenal", new ArsenalCommand(this, itemFactory));

        getLogger().info("Arsenal enabled with " + config.weapons().size() + " weapon(s) configured.");
    }

    public void reloadArsenal() {
        reloadConfig();
        config = loadConfigValues();
    }

    public ArsenalConfig getArsenalConfig() {
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

    private ArsenalConfig loadConfigValues() {
        FileConfiguration cfg = getConfig();
        Map<String, WeaponDefinition> weapons = new LinkedHashMap<>();
        ConfigurationSection weaponsSection = cfg.getConfigurationSection("weapons");
        if (weaponsSection != null) {
            for (String id : weaponsSection.getKeys(false)) {
                ConfigurationSection s = weaponsSection.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                try {
                    Material material = Material.valueOf(s.getString("material", "STICK").toUpperCase(java.util.Locale.ROOT));
                    Material ammoMaterial = Material.valueOf(s.getString("ammo.material", "PAPER").toUpperCase(java.util.Locale.ROOT));
                    WeaponDefinition definition = new WeaponDefinition(
                            id,
                            material,
                            s.getInt("custom-model-data", 0),
                            s.getString("display-name", id),
                            s.getDouble("damage.base", 4.0),
                            s.getDouble("damage.headshot-multiplier", 2.0),
                            s.getDouble("damage.limb-multiplier", 0.75),
                            s.getInt("magazine-size", 30),
                            s.getDouble("reload-seconds", 2.0),
                            s.getLong("fire-cooldown-ms", 150),
                            s.getDouble("max-range", 50.0),
                            s.getDouble("spread.base-degrees", 1.0),
                            s.getDouble("spread.max-degrees", 6.0),
                            s.getDouble("spread.growth-per-shot-degrees", 0.5),
                            s.getDouble("spread.decay-per-second-degrees", 4.0),
                            ammoMaterial,
                            s.getString("ammo.display-name", id + " Magazine")
                    );
                    weapons.put(id.toLowerCase(java.util.Locale.ROOT), definition);
                } catch (IllegalArgumentException ex) {
                    getLogger().warning("Skipping weapon '" + id + "' - invalid material: " + ex.getMessage());
                }
            }
        }
        return new ArsenalConfig(weapons);
    }
}
