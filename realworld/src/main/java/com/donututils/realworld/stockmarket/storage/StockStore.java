package com.donututils.realworld.stockmarket.storage;

import com.donututils.realworld.stockmarket.model.Stock;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Loads/saves every stock's full live state (definition + current price/day stats) to stocks.yml. */
public final class StockStore {

    private final File file;
    private final Logger logger;

    public StockStore(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "stocks.yml");
        this.logger = logger;
    }

    public Map<String, Stock> load() {
        Map<String, Stock> stocks = new LinkedHashMap<>();
        if (!file.exists()) {
            return stocks;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException ex) {
            logger.log(Level.SEVERE, "Failed to load stocks.yml - starting with no stocks loaded", ex);
            return stocks;
        }

        ConfigurationSection stocksSection = yaml.getConfigurationSection("stocks");
        if (stocksSection == null) {
            return stocks;
        }

        for (String symbol : stocksSection.getKeys(false)) {
            ConfigurationSection s = stocksSection.getConfigurationSection(symbol);
            if (s == null) {
                continue;
            }
            Stock stock = new Stock(
                    symbol,
                    s.getString("name", symbol),
                    s.getString("sector", "General"),
                    s.getDouble("price", 100.0),
                    s.getLong("sharesOutstanding", 1_000_000L),
                    s.getDouble("drift", 0.0),
                    s.getDouble("volatility", 0.01)
            );
            stock.setPrice(s.getDouble("price", stock.price()));
            stock.setPreviousClose(s.getDouble("previousClose", stock.previousClose()));
            stock.setDayOpen(s.getDouble("dayOpen", stock.dayOpen()));
            stock.setDayHigh(s.getDouble("dayHigh", stock.dayHigh()));
            stock.setDayLow(s.getDouble("dayLow", stock.dayLow()));
            stock.setVolumeToday(s.getLong("volumeToday", 0));
            stock.setDividendYieldPerPayout(s.getDouble("dividendYieldPerPayout", 0));
            stock.setDividendIntervalMillis(s.getLong("dividendIntervalMillis", 0));
            stock.setLastDividendAtMillis(s.getLong("lastDividendAtMillis", 0));
            stock.restoreHalt(s.getBoolean("halted", false), s.getLong("haltedUntilMillis", 0));
            stock.setDelisted(s.getBoolean("delisted", false));
            stock.setDayStartMillis(s.getLong("dayStartMillis", System.currentTimeMillis()));
            stock.setMaterialName(s.getString("material", "PAPER"));
            stocks.put(symbol.toUpperCase(), stock);
        }
        return stocks;
    }

    public synchronized void save(Map<String, Stock> stocks) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Stock stock : stocks.values()) {
            String path = "stocks." + stock.symbol();
            yaml.set(path + ".name", stock.name());
            yaml.set(path + ".sector", stock.sector());
            yaml.set(path + ".material", stock.materialName());
            yaml.set(path + ".price", stock.price());
            yaml.set(path + ".previousClose", stock.previousClose());
            yaml.set(path + ".dayOpen", stock.dayOpen());
            yaml.set(path + ".dayHigh", stock.dayHigh());
            yaml.set(path + ".dayLow", stock.dayLow());
            yaml.set(path + ".volumeToday", stock.volumeToday());
            yaml.set(path + ".sharesOutstanding", stock.sharesOutstanding());
            yaml.set(path + ".drift", stock.drift());
            yaml.set(path + ".volatility", stock.volatility());
            yaml.set(path + ".dividendYieldPerPayout", stock.dividendYieldPerPayout());
            yaml.set(path + ".dividendIntervalMillis", stock.dividendIntervalMillis());
            yaml.set(path + ".lastDividendAtMillis", stock.lastDividendAtMillis());
            yaml.set(path + ".halted", stock.halted());
            yaml.set(path + ".haltedUntilMillis", stock.haltedUntilMillis());
            yaml.set(path + ".delisted", stock.delisted());
            yaml.set(path + ".dayStartMillis", stock.dayStartMillis());
        }
        try {
            file.getParentFile().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to save stocks.yml", ex);
        }
    }
}
