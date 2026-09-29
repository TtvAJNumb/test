package com.donututils.donutrep.market.config;

import java.util.List;
import java.util.Map;

/** The whole /shop tree loaded from config.yml's "market" section: the root menu's title and category
 * buttons (each pointing at one entry in {@code menus}, keyed by category id), plus the per-transaction
 * quantity cap shared by every item that doesn't set its own {@link ShopItem#maxQuantity()}. */
public record MarketConfig(
        String rootTitle,
        List<ShopCategory> categories,
        Map<String, ShopMenu> menus,
        int maxQuantityPerTransaction
) {
}
