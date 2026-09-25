package com.donututils.punishhistory.notes;

import com.donututils.punishhistory.model.Note;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Freeform staff notes per player, stored at {@code plugins/PunishmentHistoryGUI/notes/<uuid>.yml}.
 * UltimateDonutSmp's PunishmentManager only models BAN/MUTE/VOICE_MUTE/WARN/KICK/BLACKLIST - there
 * is no "note" concept there, so this plugin is the source of truth for notes.
 */
public final class NoteStore {

    private final Plugin plugin;
    private final File folder;
    private final int maxPerPlayer;

    public NoteStore(Plugin plugin, int maxPerPlayer) {
        this.plugin = plugin;
        this.maxPerPlayer = Math.max(1, maxPerPlayer);
        this.folder = new File(plugin.getDataFolder(), "notes");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Could not create notes folder at " + folder.getAbsolutePath());
        }
    }

    public void addNote(UUID uuid, Note note) {
        File file = fileFor(uuid);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> existing = config.getMapList("notes");

        List<Map<?, ?>> updated = new ArrayList<>();
        updated.add(note.toMap());
        for (Map<?, ?> raw : existing) {
            if (updated.size() >= maxPerPlayer) {
                break;
            }
            updated.add(raw);
        }

        config.set("notes", updated);
        save(file, config, uuid);
    }

    /** @return true if an entry existed at that index and was removed. */
    public boolean removeNote(UUID uuid, int index) {
        File file = fileFor(uuid);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> existing = new ArrayList<>(config.getMapList("notes"));
        if (index < 0 || index >= existing.size()) {
            return false;
        }
        existing.remove(index);
        config.set("notes", existing);
        save(file, config, uuid);
        return true;
    }

    public List<Note> getNotes(UUID uuid) {
        File file = fileFor(uuid);
        if (!file.exists()) {
            return List.of();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> raw = config.getMapList("notes");
        List<Note> notes = new ArrayList<>();
        for (Map<?, ?> map : raw) {
            notes.add(Note.fromMap(map));
        }
        return notes;
    }

    public int countNotes(UUID uuid) {
        return getNotes(uuid).size();
    }

    /** Every player uuid that currently has at least one note on file. */
    public List<UUID> allNotedPlayers() {
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        List<UUID> uuids = new ArrayList<>();
        if (files == null) {
            return uuids;
        }
        for (File file : files) {
            String name = file.getName();
            try {
                uuids.add(UUID.fromString(name.substring(0, name.length() - ".yml".length())));
            } catch (IllegalArgumentException ignored) {
                // not a uuid-named file, skip
            }
        }
        return uuids;
    }

    private void save(File file, YamlConfiguration config, UUID uuid) {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save notes for " + uuid + ": " + e.getMessage());
        }
    }

    private File fileFor(UUID uuid) {
        return new File(folder, uuid + ".yml");
    }
}
