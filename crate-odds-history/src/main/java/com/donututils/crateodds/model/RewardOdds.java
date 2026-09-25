package com.donututils.crateodds.model;

import org.bukkit.Material;

import java.util.List;

/**
 * A single crate reward slot resolved into something we can render in a menu,
 * plus its computed drop chance (weight / sum of all positive weights in the crate).
 */
public record RewardOdds(
        String rewardId,
        String displayName,
        List<String> lore,
        Material icon,
        int amount,
        int weight,
        double percent,
        String grantSummary
) {
}
