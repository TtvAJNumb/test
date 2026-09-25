package com.donututils.crateodds.history;

import com.donututils.crateodds.model.HistoryEntry;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persists each player's recent crate openings to {@code plugins/CrateOddsHistory/history/<uuid>.yml}.
 * UltimateDonutSmp itself keeps no log of crate opens (only current key balances and which
 * blocks are bound to which crate), so this plugin is the only place this history lives.
 */
public final class HistoryStore {

    private final Plugin plugin;
    private final File folder;
    private final int maxEntries;

    public HistoryStore(Plugin plugin, int maxEntries) {
        this.plugin = plugin;
        this.maxEntries = Math.max(1, maxEntries);
        this.folder = new File(plugin.getDataFolder(), "history");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Could not create history folder at " + folder.getAbsolutePath());
        }
    }

    public void addEntry(UUID uuid, HistoryEntry entry) {
        File file = fileFor(uuid);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> existing = config.getMapList("entries");

        List<Map<?, ?>> updated = new ArrayList<>();
        updated.add(entry.toMap());
        for (Map<?, ?> raw : existing) {
            if (updated.size() >= maxEntries) {
                break;
            }
            updated.add(raw);
        }

        config.set("entries", updated);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save crate history for " + uuid + ": " + e.getMessage());
        }
    }

    public List<HistoryEntry> getEntries(UUID uuid) {
        File file = fileFor(uuid);
        if (!file.exists()) {
            return List.of();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> raw = config.getMapList("entries");
        List<HistoryEntry> out = new ArrayList<>();
        for (Map<?, ?> map : raw) {
            out.add(HistoryEntry.fromMap(map));
        }
        return out;
    }

    private File fileFor(UUID uuid) {
        return new File(folder, uuid + ".yml");
    }
}
