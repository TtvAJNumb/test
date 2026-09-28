package com.donututils.realworld.market.config;

import java.util.Map;

/** categoryMultipliers is keyed by {@link MarketCategory#configKey()}'s dimension-level grouping
 * (wood-and-forestry, agriculture-and-crops, ores-and-stone, mob-drops, nether, end) - see config.yml's
 * "market.category-multipliers" comment for why it's per-dimension/group rather than per-leaf-category
 * (tuning 13 near-identical multipliers individually isn't useful; the nether/end leaf categories all
 * share their dimension's single multiplier). */
public record MarketConfig(
        String guiTitle,
        Map<String, Double> categoryMultipliers,
        double sellBackFraction,
        int transactionCooldownSeconds,
        int maxQuantityPerTransaction,
        Map<String, BoutiqueItem> shardBoutique,
        int priceTickIntervalSeconds,
        double buyImpactPercent,
        double sellImpactPercent,
        double decayPercentPerTick,
        double minPriceFactor,
        double maxPriceFactor
) {
    public double multiplierFor(MarketCategory category) {
        String groupKey = switch (category.dimension()) {
            case NETHER -> "nether";
            case END -> "end";
            case OVERWORLD -> category.configKey();
        };
        return categoryMultipliers.getOrDefault(groupKey, 1.0);
    }
}
