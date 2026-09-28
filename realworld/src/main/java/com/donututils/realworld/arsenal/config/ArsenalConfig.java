package com.donututils.realworld.arsenal.config;

import java.util.Map;

/** Immutable snapshot of config.yml, re-read on reload. */
public record ArsenalConfig(Map<String, WeaponDefinition> weapons) {

    public WeaponDefinition weapon(String id) {
        return weapons.get(id.toLowerCase(java.util.Locale.ROOT));
    }
}
