package com.donututils.realworld.dynamicshop.gui;

import com.donututils.realworld.dynamicshop.model.ShopItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.function.BiConsumer;

public final class ShopMenuClickListener implements Listener {

    private final PriceFormatter formatter;

    public ShopMenuClickListener(PriceFormatter formatter) {
        this.formatter = formatter;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ShopMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) {
            return;
        }

        int raw = event.getRawSlot();
        if (raw == ShopPagedMenu.PREV_SLOT) {
            if (holder.getPage() > 0) {
                ShopPagedMenu.render(holder, holder.getPage() - 1, formatter);
            }
            return;
        }
        if (raw == ShopPagedMenu.NEXT_SLOT) {
            if (holder.getPage() < holder.getTotalPages() - 1) {
                ShopPagedMenu.render(holder, holder.getPage() + 1, formatter);
            }
            return;
        }
        if (raw == ShopPagedMenu.CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        String filter = holder.getFilterSlots().get(raw);
        if (filter != null) {
            BiConsumer<Player, String> onFilterSelected = holder.getOnFilterSelected();
            if (onFilterSelected != null) {
                onFilterSelected.accept(player, filter);
            }
            return;
        }

        handleContentClick(holder, player, raw, event.isShiftClick());
    }

    private void handleContentClick(ShopMenuHolder holder, Player player, int raw, boolean shiftClick) {
        if (raw < 0 || raw >= ShopPagedMenu.PAGE_SIZE) {
            return;
        }
        int index = holder.getPage() * ShopPagedMenu.PAGE_SIZE + raw;
        List<ShopItem> items = holder.getItems();
        if (index < 0 || index >= items.size()) {
            return;
        }
        ShopItem item = items.get(index);
        if (shiftClick) {
            BiConsumer<Player, ShopItem> onSell = holder.getOnSell();
            if (onSell != null) {
                onSell.accept(player, item);
            }
        } else {
            BiConsumer<Player, ShopItem> onBuy = holder.getOnBuy();
            if (onBuy != null) {
                onBuy.accept(player, item);
            }
        }
    }
}
