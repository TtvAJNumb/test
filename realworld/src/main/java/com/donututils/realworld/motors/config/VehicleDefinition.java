package com.donututils.realworld.motors.config;

import org.bukkit.Material;

/** One configured vehicle "make/model" - a spawner key material/appearance, the vanilla entity kind
 * it rides on (BOAT or MINECART), the decorative body item worn by the invisible ArmorStand that
 * tags along, and its fuel/wear economics. */
public record VehicleDefinition(
        String id,
        Kind kind,
        Material keyMaterial,
        int keyCustomModelData,
        String keyDisplayName,
        Material bodyHelmetMaterial,
        int bodyHelmetCustomModelData,
        String displayName,
        double maxFuel,
        double fuelDrainPerSecond,
        double maxWear,
        double wearPerBlock,
        double refuelCostPerUnit,
        double repairCostPerWear
) {
    public enum Kind {
        BOAT,
        MINECART
    }
}
