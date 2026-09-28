package com.donututils.realworld.dynamicshop.engine;

import com.donututils.realworld.dynamicshop.model.ShopItem;

/**
 * Adjusts each item's price once per tick based on the net buy/sell pressure recorded since the
 * last tick (see {@link ShopItem#addNetFlow}) - more buying pushes the price up, more selling pushes
 * it down, exactly the mechanic Auto-Tune describes, clamped by the item's own volatility setting and
 * min/max bounds so no single tick (or single huge transaction) can send a price to zero or infinity.
 */
public final class PricingEngine {

    private final ShopItemRegistry registry;

    public PricingEngine(ShopItemRegistry registry) {
        this.registry = registry;
    }

    public void tick() {
        for (ShopItem item : registry.all()) {
            double netFlow = item.takePendingNetFlow();
            if (netFlow == 0) {
                continue;
            }
            // "Depth" scales with the item's own price - cheap bulk items need more volume to move
            // their price by the same percentage as an expensive item.
            double depth = Math.max(1.0, item.basePrice() * 50);
            double rawImpact = netFlow / depth;
            double cap = item.volatility();
            double clampedImpact = Math.max(-cap, Math.min(cap, rawImpact));
            double newPrice = item.currentPrice() * (1 + clampedImpact);
            newPrice = Math.max(item.minPrice(), Math.min(item.maxPrice(), newPrice));
            item.setCurrentPrice(newPrice);
        }
    }
}
