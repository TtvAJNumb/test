package com.donututils.realworld.market.pricing;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Persists every catalog item's current dynamic-price factor (1.0 = base price, drifting up when
 * players buy and down when they sell - see {@link MarketPricingEngine}) to a flat YAML file, the
 * same persistence style as this project's other flat-file stores (e.g. StockMarket's StockStore).
 * Keyed by Material name since the catalog itself is generated, not config-defined. */
public final class MarketPriceStore {

    private final File file;
    private final Logger logger;
    private final Map<String, Double> factors = new ConcurrentHashMap<>();

    public MarketPriceStore(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "market-prices.yml");
        this.logger = logger;
        load();
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            factors.put(key, yaml.getDouble(key, 1.0));
        }
    }

    public double factorFor(String materialName) {
        return factors.getOrDefault(materialName, 1.0);
    }

    public void setFactor(String materialName, double factor) {
        factors.put(materialName, factor);
    }

    /** Nudges every currently-tracked factor a step closer to {@code target} (e.g. 1.0 equilibrium). */
    public void decayAllToward(double target, double stepFraction) {
        for (Map.Entry<String, Double> entry : factors.entrySet()) {
            double current = entry.getValue();
            factors.put(entry.getKey(), current + (target - current) * stepFraction);
        }
    }

    public void saveAll() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Double> entry : factors.entrySet()) {
            yaml.set(entry.getKey(), entry.getValue());
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to save market-prices.yml", ex);
        }
    }
}
