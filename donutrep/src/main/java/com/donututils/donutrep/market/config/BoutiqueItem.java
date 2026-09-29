package com.donututils.donutrep.market.config;

/** One Shard Boutique entry - a cosmetic/black-market vanilla item, priced only in Shards. */
public record BoutiqueItem(
        String id,
        String displayName,
        String description,
        String material,
        int customModelData,
        long priceShards
) {
}
