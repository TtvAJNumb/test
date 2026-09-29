package com.donututils.donutrep.market.config;

import org.bukkit.Material;

/** One button in the root /shop menu (matches real UDS shop.yml's CATEGORIES section: END, NETHER,
 * GEAR, FOOD, SHARD, CRATE-KEYS), pointing at the {@link ShopMenu} it opens. */
public record ShopCategory(String id, String displayName, Material icon, int slot) {
}
