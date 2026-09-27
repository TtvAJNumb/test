package com.donututils.dynamicshop.storage;

import com.donututils.dynamicshop.model.PlayerShopData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PlayerDataStore {

    private final File file;
    private final Logger logger;

    public PlayerDataStore(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "player-data.yml");
        this.logger = logger;
    }

    public Map<UUID, PlayerShopData> load() {
        Map<UUID, PlayerShopData> map = new LinkedHashMap<>();
        if (!file.exists()) {
            return map;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection playersSection = yaml.getConfigurationSection("players");
        if (playersSection == null) {
            return map;
        }
        for (String key : playersSection.getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            ConfigurationSection s = playersSection.getConfigurationSection(key);
            if (s == null) {
                continue;
            }
            PlayerShopData data = new PlayerShopData();
            for (String material : s.getStringList("unlocked")) {
                data.unlock(material);
            }
            for (String material : s.getStringList("autosell")) {
                data.setAutoSell(material, true);
            }
            map.put(playerId, data);
        }
        return map;
    }

    public synchronized void save(Map<UUID, PlayerShopData> data) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerShopData> entry : data.entrySet()) {
            String path = "players." + entry.getKey();
            yaml.set(path + ".unlocked", new ArrayList<>(entry.getValue().unlockedMaterials()));
            yaml.set(path + ".autosell", new ArrayList<>(entry.getValue().autoSellMaterials()));
        }
        try {
            AtomicFiles.writeYaml(yaml, file);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to save player-data.yml", ex);
        }
    }
}
