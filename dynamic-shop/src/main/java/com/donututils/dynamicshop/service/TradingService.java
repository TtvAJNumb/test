package com.donututils.dynamicshop.service;

import com.donututils.dynamicshop.config.DynamicShopConfig;
import com.donututils.dynamicshop.config.Messages;
import com.donututils.dynamicshop.currency.CurrencyProvider;
import com.donututils.dynamicshop.currency.CurrencyRegistry;
import com.donututils.dynamicshop.engine.PlayerDataRegistry;
import com.donututils.dynamicshop.engine.ShopItemRegistry;
import com.donututils.dynamicshop.model.PlayerShopData;
import com.donututils.dynamicshop.model.ShopItem;
import com.donututils.dynamicshop.model.TransactionRecord;
import com.donututils.dynamicshop.storage.TransactionLogStore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Buy/sell logic: currency resolution, exploit-protection limits, price-pressure feedback into the
 * pricing engine, and transaction logging - all in one place so every entry point (GUI, commands,
 * auto-sell) goes through the same checks. All player-facing wording comes from messages.yml. */
public final class TradingService {

    public record TradeResult(boolean success, String message) {
    }

    private final ShopItemRegistry itemRegistry;
    private final CurrencyRegistry currencyRegistry;
    private final PlayerDataRegistry playerDataRegistry;
    private final TransactionLogStore transactionLog;
    private final Supplier<DynamicShopConfig> configSupplier;
    private final Supplier<Messages> messagesSupplier;
    private final ConcurrentHashMap<String, Long> lastTransactionMillis = new ConcurrentHashMap<>();

    public TradingService(ShopItemRegistry itemRegistry, CurrencyRegistry currencyRegistry,
                           PlayerDataRegistry playerDataRegistry, TransactionLogStore transactionLog,
                           Supplier<DynamicShopConfig> configSupplier, Supplier<Messages> messagesSupplier) {
        this.itemRegistry = itemRegistry;
        this.currencyRegistry = currencyRegistry;
        this.playerDataRegistry = playerDataRegistry;
        this.transactionLog = transactionLog;
        this.configSupplier = configSupplier;
        this.messagesSupplier = messagesSupplier;
    }

    public TradeResult buy(UUID playerId, String material, long quantity) {
        Messages msg = messagesSupplier.get();
        ShopItem item = itemRegistry.get(material);
        if (item == null) {
            return fail(msg.get("unknown-item"));
        }
        if (!item.buyEnabled()) {
            return fail(msg.get("buy-disabled", Map.of("item", item.displayName())));
        }
        if (quantity <= 0) {
            return fail(msg.get("quantity-positive"));
        }
        DynamicShopConfig config = configSupplier.get();
        if (config.purchaseRestrictionEnabled()) {
            PlayerShopData data = playerDataRegistry.get(playerId);
            if (!data.hasUnlocked(item.material())) {
                return fail(msg.get("buy-restricted", Map.of("item", item.displayName())));
            }
        }
        long maxQty = Math.min(item.maxBuyPerTransaction(), config.globalMaxQuantityPerTransaction());
        if (quantity > maxQty) {
            return fail(msg.get("max-quantity", Map.of("max", String.valueOf(maxQty))));
        }
        TradeResult cooldown = checkCooldown(playerId, item, msg, config);
        if (cooldown != null) {
            return cooldown;
        }

        CurrencyProvider currency = currencyRegistry.get(item.currency());
        if (currency == null) {
            return fail(msg.get("currency-unavailable"));
        }

        double pricePerUnit = item.currentPrice();
        double total = pricePerUnit * quantity;
        if (!currency.has(playerId, total)) {
            return fail(msg.get("buy-insufficient-funds", Map.of("currency", currency.displayName(), "total", currency.format(total))));
        }
        if (!currency.withdraw(playerId, total)) {
            return fail(msg.get("payment-failed"));
        }

        item.addNetFlow(total);
        transactionLog.append(new TransactionRecord(playerId, item.material(), "BUY", quantity, pricePerUnit, total, item.currency(), System.currentTimeMillis()));
        markCooldown(playerId, item.material());
        return success(msg.get("buy-success", tradeParams(quantity, item, currency, total)));
    }

    public TradeResult sell(UUID playerId, String material, long quantity) {
        Messages msg = messagesSupplier.get();
        ShopItem item = itemRegistry.get(material);
        if (item == null) {
            return fail(msg.get("unknown-item"));
        }
        if (!item.sellEnabled()) {
            return fail(msg.get("sell-disabled", Map.of("item", item.displayName())));
        }
        if (quantity <= 0) {
            return fail(msg.get("quantity-positive"));
        }
        DynamicShopConfig config = configSupplier.get();
        long maxQty = Math.min(item.maxSellPerTransaction(), config.globalMaxQuantityPerTransaction());
        if (quantity > maxQty) {
            return fail(msg.get("max-quantity", Map.of("max", String.valueOf(maxQty))));
        }
        TradeResult cooldown = checkCooldown(playerId, item, msg, config);
        if (cooldown != null) {
            return cooldown;
        }

        CurrencyProvider currency = currencyRegistry.get(item.currency());
        if (currency == null) {
            return fail(msg.get("currency-unavailable"));
        }

        double pricePerUnit = item.currentPrice();
        double total = pricePerUnit * quantity;
        currency.deposit(playerId, total);
        item.addNetFlow(-total);
        playerDataRegistry.get(playerId).unlock(item.material());
        transactionLog.append(new TransactionRecord(playerId, item.material(), "SELL", quantity, pricePerUnit, total, item.currency(), System.currentTimeMillis()));
        markCooldown(playerId, item.material());
        return success(msg.get("sell-success", tradeParams(quantity, item, currency, total)));
    }

    /** Used by auto-sell: no per-transaction cooldown (it can fire many times a second while mining),
     * but everything else - sellable check, currency resolution, price impact, logging - is shared. */
    public TradeResult autoSell(UUID playerId, String material, long quantity) {
        Messages msg = messagesSupplier.get();
        ShopItem item = itemRegistry.get(material);
        if (item == null || !item.sellEnabled()) {
            return fail("Not sellable.");
        }
        CurrencyProvider currency = currencyRegistry.get(item.currency());
        if (currency == null) {
            return fail(msg.get("currency-unavailable"));
        }
        double pricePerUnit = item.currentPrice();
        double total = pricePerUnit * quantity;
        currency.deposit(playerId, total);
        item.addNetFlow(-total);
        playerDataRegistry.get(playerId).unlock(item.material());
        transactionLog.append(new TransactionRecord(playerId, item.material(), "AUTOSELL", quantity, pricePerUnit, total, item.currency(), System.currentTimeMillis()));
        return success(msg.get("autosell-success", tradeParams(quantity, item, currency, total)));
    }

    private Map<String, String> tradeParams(long quantity, ShopItem item, CurrencyProvider currency, double total) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("quantity", String.valueOf(quantity));
        params.put("item", item.displayName());
        params.put("total", currency.format(total));
        return params;
    }

    private TradeResult checkCooldown(UUID playerId, ShopItem item, Messages msg, DynamicShopConfig config) {
        String key = playerId + ":" + item.material();
        long now = System.currentTimeMillis();
        Long last = lastTransactionMillis.get(key);
        long cooldownMillis = config.transactionCooldownSeconds() * 1000L;
        if (last != null && now - last < cooldownMillis) {
            return fail(msg.get("cooldown", Map.of("item", item.displayName())));
        }
        return null;
    }

    private void markCooldown(UUID playerId, String material) {
        lastTransactionMillis.put(playerId + ":" + material, System.currentTimeMillis());
    }

    private static TradeResult success(String message) {
        return new TradeResult(true, message);
    }

    private static TradeResult fail(String message) {
        return new TradeResult(false, message);
    }
}
