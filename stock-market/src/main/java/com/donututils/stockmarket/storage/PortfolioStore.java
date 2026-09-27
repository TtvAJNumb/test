package com.donututils.stockmarket.storage;

import com.donututils.stockmarket.model.Holding;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** One small YAML file per player: their current holdings, keyed by stock symbol. */
public final class PortfolioStore {

    private final File portfolioDirectory;
    private final Logger logger;

    public PortfolioStore(File dataFolder, Logger logger) {
        this.portfolioDirectory = new File(dataFolder, "portfolios");
        this.logger = logger;
    }

    public synchronized Map<String, Holding> load(UUID playerId) {
        Map<String, Holding> holdings = new LinkedHashMap<>();
        File file = fileFor(playerId);
        if (!file.exists()) {
            return holdings;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException ex) {
            logger.log(Level.WARNING, "Failed to load portfolio for " + playerId, ex);
            return holdings;
        }

        ConfigurationSection holdingsSection = yaml.getConfigurationSection("holdings");
        if (holdingsSection == null) {
            return holdings;
        }
        for (String symbol : holdingsSection.getKeys(false)) {
            ConfigurationSection s = holdingsSection.getConfigurationSection(symbol);
            if (s == null) {
                continue;
            }
            long shares = s.getLong("shares", 0);
            double costBasis = s.getDouble("costBasisTotal", 0);
            if (shares > 0) {
                holdings.put(symbol.toUpperCase(), new Holding(shares, costBasis));
            }
        }
        return holdings;
    }

    public synchronized void save(UUID playerId, Map<String, Holding> holdings) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Holding> entry : holdings.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            String path = "holdings." + entry.getKey();
            yaml.set(path + ".shares", entry.getValue().shares());
            yaml.set(path + ".costBasisTotal", entry.getValue().costBasisTotal());
        }
        try {
            portfolioDirectory.mkdirs();
            yaml.save(fileFor(playerId));
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to save portfolio for " + playerId, ex);
        }
    }

    /** Every player UUID that has ever had a portfolio file - used for leaderboards/dividends/splits. */
    public List<UUID> listAllPlayerIds() {
        List<UUID> ids = new ArrayList<>();
        File[] files = portfolioDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return ids;
        }
        for (File file : files) {
            String name = file.getName();
            String uuidPart = name.substring(0, name.length() - ".yml".length());
            try {
                ids.add(UUID.fromString(uuidPart));
            } catch (IllegalArgumentException ignored) {
                // not a UUID-named file, skip
            }
        }
        return ids;
    }

    private File fileFor(UUID playerId) {
        return new File(portfolioDirectory, playerId.toString() + ".yml");
    }
}
