package com.donututils.donutrep.sell;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

/** Real per-material sell prices, loaded from the bundled worth.yml (flattened from real UDS's own
 * worth.yml) - a separate, independent price list from the /shop buy catalog, exactly like real UDS
 * keeps buying (shop.yml) and selling (worth.yml) as two unrelated systems. */
public final class WorthStore {

    private final Map<Material, Double> prices = new HashMap<>();

    public WorthStore(Plugin plugin) {
        try (InputStream in = plugin.getResource("worth.yml")) {
            if (in == null) {
                plugin.getLogger().warning("worth.yml resource is missing - /sell and /worth will have no prices.");
                return;
            }
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            ConfigurationSection section = yaml.getConfigurationSection("prices");
            if (section == null) {
                return;
            }
            for (String key : section.getKeys(false)) {
                try {
                    Material material = Material.valueOf(key.toUpperCase(Locale.ROOT));
                    prices.put(material, section.getDouble(key, 0.0));
                } catch (IllegalArgumentException ignored) {
                    // Material doesn't exist on this server version - skip it.
                }
            }
            plugin.getLogger().info("Loaded " + prices.size() + " item worth price(s).");
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load worth.yml", ex);
        }
    }

    public boolean hasPrice(Material material) {
        return prices.containsKey(material);
    }

    public double priceFor(Material material) {
        return prices.getOrDefault(material, 0.0);
    }

    public Map<Material, Double> all() {
        return prices;
    }
}
