package com.donututils.realworld.crates;

import org.bukkit.Material;

import java.util.List;

public record CrateDefinition(
        String id,
        String displayName,
        Material keyMaterial,
        int keyCustomModelData,
        List<CrateReward> rewards
) {
}
