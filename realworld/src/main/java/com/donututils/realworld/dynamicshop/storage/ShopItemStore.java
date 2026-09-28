package com.donututils.realworld.dynamicshop.storage;

import com.donututils.realworld.dynamicshop.model.ShopItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Loads/saves every shop item's config and live price to shop-items.yml, atomically. */
public final class ShopItemStore {

    private final File file;
    private final Logger logger;

    public ShopItemStore(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "shop-items.yml");
        this.logger = logger;
    }

    public Map<String, ShopItem> load() {
        Map<String, ShopItem> items = new LinkedHashMap<>();
        if (!file.exists()) {
            return items;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection itemsSection = yaml.getConfigurationSection("items");
        if (itemsSection == null) {
            return items;
        }
        for (String material : itemsSection.getKeys(false)) {
            ConfigurationSection s = itemsSection.getConfigurationSection(material);
            if (s == null) {
                continue;
            }
            ShopItem item = new ShopItem(material, s.getString("currency", "money"));
            item.setCategory(s.getString("category", "General"));
            item.setDisplayName(s.getString("displayName", material));
            item.setBasePrice(s.getDouble("basePrice", 1.0));
            item.setCurrentPrice(s.getDouble("currentPrice", item.basePrice()));
            item.setMinPrice(s.getDouble("minPrice", Math.max(0.01, item.basePrice() * 0.1)));
            item.setMaxPrice(s.getDouble("maxPrice", item.basePrice() * 10));
            item.setVolatility(s.getDouble("volatility", 0.01));
            item.setBuyEnabled(s.getBoolean("buyEnabled", true));
            item.setSellEnabled(s.getBoolean("sellEnabled", true));
            item.setMaxBuyPerTransaction(s.getInt("maxBuyPerTransaction", 64));
            item.setMaxSellPerTransaction(s.getInt("maxSellPerTransaction", 64));
            items.put(material.toUpperCase(Locale.ROOT), item);
        }
        return items;
    }

    public synchronized void save(Map<String, ShopItem> items) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (ShopItem item : items.values()) {
            String path = "items." + item.material();
            yaml.set(path + ".currency", item.currency());
            yaml.set(path + ".category", item.category());
            yaml.set(path + ".displayName", item.displayName());
            yaml.set(path + ".basePrice", item.basePrice());
            yaml.set(path + ".currentPrice", item.currentPrice());
            yaml.set(path + ".minPrice", item.minPrice());
            yaml.set(path + ".maxPrice", item.maxPrice());
            yaml.set(path + ".volatility", item.volatility());
            yaml.set(path + ".buyEnabled", item.buyEnabled());
            yaml.set(path + ".sellEnabled", item.sellEnabled());
            yaml.set(path + ".maxBuyPerTransaction", item.maxBuyPerTransaction());
            yaml.set(path + ".maxSellPerTransaction", item.maxSellPerTransaction());
        }
        try {
            AtomicFiles.writeYaml(yaml, file);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to save shop-items.yml", ex);
        }
    }
}
