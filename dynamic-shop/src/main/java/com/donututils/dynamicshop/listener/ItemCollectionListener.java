package com.donututils.dynamicshop.listener;

import com.donututils.dynamicshop.config.DynamicShopConfig;
import com.donututils.dynamicshop.engine.PlayerDataRegistry;
import com.donututils.dynamicshop.engine.ShopItemRegistry;
import com.donututils.dynamicshop.model.PlayerShopData;
import com.donututils.dynamicshop.model.ShopItem;
import com.donututils.dynamicshop.service.TradingService;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Supplier;

/**
 * On every item pickup: marks the material "unlocked" for the purchase-restriction feature (a player
 * who has genuinely collected something in the world can always buy more of it later), and - if the
 * player has auto-sell turned on for that material and the feature is enabled - sells it on the spot
 * instead of letting it enter their inventory.
 */
public final class ItemCollectionListener implements Listener {

    private final ShopItemRegistry itemRegistry;
    private final PlayerDataRegistry playerDataRegistry;
    private final TradingService tradingService;
    private final Supplier<DynamicShopConfig> configSupplier;

    public ItemCollectionListener(ShopItemRegistry itemRegistry, PlayerDataRegistry playerDataRegistry,
                                   TradingService tradingService, Supplier<DynamicShopConfig> configSupplier) {
        this.itemRegistry = itemRegistry;
        this.playerDataRegistry = playerDataRegistry;
        this.tradingService = tradingService;
        this.configSupplier = configSupplier;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }
        ItemStack stack = event.getItem().getItemStack();
        if (stack == null) {
            return;
        }
        String material = stack.getType().name();

        PlayerShopData data = playerDataRegistry.get(player.getUniqueId());
        data.unlock(material);

        DynamicShopConfig config = configSupplier.get();
        if (!config.autoSellFeatureEnabled() || !data.isAutoSellEnabled(material)) {
            return;
        }
        ShopItem item = itemRegistry.get(material);
        if (item == null || !item.sellEnabled()) {
            return;
        }

        event.setCancelled(true);
        TradingService.TradeResult result = tradingService.autoSell(player.getUniqueId(), material, stack.getAmount());
        if (!result.success()) {
            // Auto-sell failing (e.g. currency briefly unavailable) shouldn't destroy the item -
            // let it drop normally instead of silently vanishing.
            event.setCancelled(false);
        }
    }
}
