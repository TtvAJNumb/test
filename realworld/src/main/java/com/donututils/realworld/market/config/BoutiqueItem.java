package com.donututils.realworld.market.config;

/** One Shard Boutique entry - a cosmetic weapon/vehicle skin or a black-market vanilla item, priced
 * only in Shards. Exactly one of baseWeapon/baseVehicle/material is set, matching which kind it is. */
public record BoutiqueItem(
        String id,
        String displayName,
        String description,
        String baseWeapon,
        String baseVehicle,
        String material,
        int customModelData,
        long priceShards
) {
    public boolean isWeaponSkin() {
        return baseWeapon != null;
    }

    public boolean isVehicleSkin() {
        return baseVehicle != null;
    }

    public boolean isVanillaItem() {
        return material != null;
    }
}
