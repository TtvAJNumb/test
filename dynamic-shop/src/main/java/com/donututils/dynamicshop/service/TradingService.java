package com.donututils.dynamicshop.service;

import com.donututils.dynamicshop.config.DynamicShopConfig;
import com.donututils.dynamicshop.currency.CurrencyProvider;
import com.donututils.dynamicshop.currency.CurrencyRegistry;
import com.donututils.dynamicshop.engine.PlayerDataRegistry;
import com.donututils.dynamicshop.engine.ShopItemRegistry;
import com.donututils.dynamicshop.model.PlayerShopData;
import com.donututils.dynamicshop.model.ShopItem;
import com.donututils.dynamicshop.model.TransactionRecord;
import com.donututils.dynamicshop.storage.TransactionLogStore;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Buy/sell logic: currency resolution, exploit-protection limits, price-pressure feedback into the
 * pricing engine, and transaction logging - all in one place so every entry point (GUI, commands,
 * auto-sell) goes through the same checks. */
public final class TradingService {

    public record TradeResult(boolean success, String message) {
    }

    private final ShopItemRegistry itemRegistry;
    private final CurrencyRegistry currencyRegistry;
    private final PlayerDataRegistry playerDataRegistry;
    private final TransactionLogStore transactionLog;
    private final Supplier<DynamicShopConfig> configSupplier;
    private final ConcurrentHashMap<String, Long> lastTransactionMillis = new ConcurrentHashMap<>();

    public TradingService(ShopItemRegistry itemRegistry, CurrencyRegistry currencyRegistry,
                           PlayerDataRegistry playerDataRegistry, TransactionLogStore transactionLog,
                           Supplier<DynamicShopConfig> configSupplier) {
        this.itemRegistry = itemRegistry;
        this.currencyRegistry = currencyRegistry;
        this.playerDataRegistry = playerDataRegistry;
        this.transactionLog = transactionLog;
        this.configSupplier = configSupplier;
    }

    public TradeResult buy(UUID playerId, String material, long quantity) {
        ShopItem item = itemRegistry.get(material);
        if (item == null) {
            return new TradeResult(false, "That item isn't sold here.");
        }
        if (!item.buyEnabled()) {
            return new TradeResult(false, item.displayName() + " isn't currently buyable.");
        }
        if (quantity <= 0) {
            return new TradeResult(false, "Quantity must be positive.");
        }
        DynamicShopConfig config = configSupplier.get();
        if (config.purchaseRestrictionEnabled()) {
            PlayerShopData data = playerDataRegistry.get(playerId);
            if (!data.hasUnlocked(item.material())) {
                return new TradeResult(false, "You need to have collected " + item.displayName() + " in the world before you can buy it.");
            }
        }
        long maxQty = Math.min(item.maxBuyPerTransaction(), config.globalMaxQuantityPerTransaction());
        if (quantity > maxQty) {
            return new TradeResult(false, "You can only buy up to " + maxQty + " at a time.");
        }
        TradeResult cooldown = checkCooldown(playerId, item.material(), config);
        if (cooldown != null) {
            return cooldown;
        }

        CurrencyProvider currency = currencyRegistry.get(item.currency());
        if (currency == null) {
            return new TradeResult(false, "That item's currency isn't available right now.");
        }

        double pricePerUnit = item.currentPrice();
        double total = pricePerUnit * quantity;
        if (!currency.has(playerId, total)) {
            return new TradeResult(false, "You don't have enough " + currency.displayName() + " for that (" + currency.format(total) + " needed).");
        }
        if (!currency.withdraw(playerId, total)) {
            return new TradeResult(false, "The payment failed - try again.");
        }

        item.addNetFlow(total);
        transactionLog.append(new TransactionRecord(playerId, item.material(), "BUY", quantity, pricePerUnit, total, item.currency(), System.currentTimeMillis()));
        markCooldown(playerId, item.material());
        return new TradeResult(true, "Bought " + quantity + "x " + item.displayName() + " for " + currency.format(total) + ".");
    }

    public TradeResult sell(UUID playerId, String material, long quantity) {
        ShopItem item = itemRegistry.get(material);
        if (item == null) {
            return new TradeResult(false, "That item isn't sold here.");
        }
        if (!item.sellEnabled()) {
            return new TradeResult(false, item.displayName() + " isn't currently sellable.");
        }
        if (quantity <= 0) {
            return new TradeResult(false, "Quantity must be positive.");
        }
        DynamicShopConfig config = configSupplier.get();
        long maxQty = Math.min(item.maxSellPerTransaction(), config.globalMaxQuantityPerTransaction());
        if (quantity > maxQty) {
            return new TradeResult(false, "You can only sell up to " + maxQty + " at a time.");
        }
        TradeResult cooldown = checkCooldown(playerId, item.material(), config);
        if (cooldown != null) {
            return cooldown;
        }

        CurrencyProvider currency = currencyRegistry.get(item.currency());
        if (currency == null) {
            return new TradeResult(false, "That item's currency isn't available right now.");
        }

        double pricePerUnit = item.currentPrice();
        double total = pricePerUnit * quantity;
        currency.deposit(playerId, total);
        item.addNetFlow(-total);
        playerDataRegistry.get(playerId).unlock(item.material());
        transactionLog.append(new TransactionRecord(playerId, item.material(), "SELL", quantity, pricePerUnit, total, item.currency(), System.currentTimeMillis()));
        markCooldown(playerId, item.material());
        return new TradeResult(true, "Sold " + quantity + "x " + item.displayName() + " for " + currency.format(total) + ".");
    }

    /** Used by auto-sell: no per-transaction cooldown (it can fire many times a second while mining),
     * but everything else - sellable check, currency resolution, price impact, logging - is shared. */
    public TradeResult autoSell(UUID playerId, String material, long quantity) {
        ShopItem item = itemRegistry.get(material);
        if (item == null || !item.sellEnabled()) {
            return new TradeResult(false, "Not sellable.");
        }
        CurrencyProvider currency = currencyRegistry.get(item.currency());
        if (currency == null) {
            return new TradeResult(false, "Currency unavailable.");
        }
        double pricePerUnit = item.currentPrice();
        double total = pricePerUnit * quantity;
        currency.deposit(playerId, total);
        item.addNetFlow(-total);
        playerDataRegistry.get(playerId).unlock(item.material());
        transactionLog.append(new TransactionRecord(playerId, item.material(), "AUTOSELL", quantity, pricePerUnit, total, item.currency(), System.currentTimeMillis()));
        return new TradeResult(true, "Auto-sold " + quantity + "x " + item.displayName() + " for " + currency.format(total) + ".");
    }

    private TradeResult checkCooldown(UUID playerId, String material, DynamicShopConfig config) {
        String key = playerId + ":" + material;
        long now = System.currentTimeMillis();
        Long last = lastTransactionMillis.get(key);
        long cooldownMillis = config.transactionCooldownSeconds() * 1000L;
        if (last != null && now - last < cooldownMillis) {
            return new TradeResult(false, "Slow down - wait a moment before trading " + material + " again.");
        }
        return null;
    }

    private void markCooldown(UUID playerId, String material) {
        lastTransactionMillis.put(playerId + ":" + material, System.currentTimeMillis());
    }
}
