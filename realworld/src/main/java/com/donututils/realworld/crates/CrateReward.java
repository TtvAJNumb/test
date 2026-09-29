package com.donututils.realworld.crates;

import org.bukkit.Material;

/** One weighted entry in a crate's reward pool. Exactly one of material/money/shards is set,
 * matching {@link #kind()}. amountMin/amountMax is inclusive on both ends (a fixed amount just sets
 * both the same). */
public record CrateReward(Kind kind, Material material, double moneyAmount, long shardsAmount,
                           int itemAmountMin, int itemAmountMax, String displayName, int weight) {
    public enum Kind { ITEM, MONEY, SHARDS }
}
