package com.donututils.donutrep.market.config;

import java.util.List;

/** One category's sub-menu (END-MENU, NETHER-MENU, GEAR-MENU, FOOD-MENU, SHARD-MENU, CRATE-KEYS-MENU
 * in real UDS shop.yml): a 27-slot inventory title plus its fixed-slot items. */
public record ShopMenu(String title, List<ShopItem> items) {
}
