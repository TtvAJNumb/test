package com.donututils.dynamicshop.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

/** Loads player-facing trade message templates from messages.yml, with {placeholder} substitution. */
public final class Messages {

    private final FileConfiguration config;

    public Messages(FileConfiguration config) {
        this.config = config;
    }

    public String get(String key) {
        return config.getString(key, key);
    }

    public String get(String key, Map<String, String> placeholders) {
        String template = config.getString(key, key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            template = template.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return template;
    }
}
