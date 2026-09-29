package com.donututils.donutrep.market.pricing;

import com.donututils.donutrep.market.catalog.ItemCatalog;
import com.donututils.donutrep.market.config.CatalogEntry;
import com.donututils.donutrep.market.config.MarketConfig;
import org.bukkit.Material;

import java.util.function.Supplier;

/**
 * Dynamic supply/demand pricing for the vanilla catalog: every material has a price factor (1.0 =
 * base price) that nudges up when players buy it and down when they sell it, then drifts back toward
 * 1.0 over time - the same idea as the old DynamicShop's pricing engine and StockMarket's random-walk
 * model, just simpler (no volatility/random component, purely buy/sell-pressure-driven). Prices are
 * NOT static: buying a lot of one material in a short window measurably raises what it costs next,
 * exactly like a real market with limited supply.
 */
public final class MarketPricingEngine {

    private final MarketPriceStore store;
    private final Supplier<MarketConfig> configSupplier;

    public MarketPricingEngine(MarketPriceStore store, Supplier<MarketConfig> configSupplier) {
        this.store = store;
        this.configSupplier = configSupplier;
    }

    public double buyPrice(CatalogEntry entry) {
        MarketConfig config = configSupplier.get();
        double base = ItemCatalog.basePriceForTier(entry.rarityTier()) * config.multiplierFor(entry.category());
        return base * store.factorFor(entry.material().name());
    }

    public double sellPrice(CatalogEntry entry) {
        return buyPrice(entry) * configSupplier.get().sellBackFraction();
    }

    /** Call right after a purchase completes - nudges the price up for the next buyer. */
    public void onBuy(Material material, int quantity) {
        MarketConfig config = configSupplier.get();
        adjust(material, quantity * (config.buyImpactPercent() / 100.0), config);
    }

    /** Call right after a sale completes - nudges the price down for the next buyer. */
    public void onSell(Material material, int quantity) {
        MarketConfig config = configSupplier.get();
        adjust(material, -quantity * (config.sellImpactPercent() / 100.0), config);
    }

    private void adjust(Material material, double delta, MarketConfig config) {
        String key = material.name();
        double updated = store.factorFor(key) + delta;
        updated = Math.max(config.minPriceFactor(), Math.min(config.maxPriceFactor(), updated));
        store.setFactor(key, updated);
    }

    /** Scheduled periodically: pulls every known price factor back toward 1.0 (equilibrium) so a
     * temporary buying/selling spike doesn't permanently distort a price forever. Untouched items
     * stay implicitly at 1.0 already, so only factors actually tracked in the store need decaying. */
    public void tick() {
        double decay = configSupplier.get().decayPercentPerTick() / 100.0;
        store.decayAllToward(1.0, decay);
    }

    public void saveAll() {
        store.saveAll();
    }
}
