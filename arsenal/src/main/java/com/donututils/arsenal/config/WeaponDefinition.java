package com.donututils.arsenal.config;

import org.bukkit.Material;

public record WeaponDefinition(
        String id,
        Material material,
        int customModelData,
        String displayName,
        double baseDamage,
        double headshotMultiplier,
        double limbMultiplier,
        int magazineSize,
        double reloadSeconds,
        long fireCooldownMs,
        double maxRange,
        double baseSpreadDegrees,
        double maxSpreadDegrees,
        double growthPerShotDegrees,
        double decayPerSecondDegrees,
        Material ammoMaterial,
        String ammoDisplayName
) {
}
