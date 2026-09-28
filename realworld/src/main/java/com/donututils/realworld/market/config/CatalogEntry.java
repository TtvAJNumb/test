package com.donututils.realworld.market.config;

import org.bukkit.Material;

/** One generated catalog line: a vanilla Material, the leaf category it was sorted into, and a
 * 1(common)-5(rarest) rarity tier used to look up its base price - see
 * {@link com.donututils.realworld.market.catalog.ItemCatalog} for how both are assigned. */
public record CatalogEntry(Material material, MarketCategory category, int rarityTier) {
}
