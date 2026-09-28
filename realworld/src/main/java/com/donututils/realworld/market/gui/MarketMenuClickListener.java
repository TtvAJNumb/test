package com.donututils.realworld.market.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.function.Consumer;

public final class MarketMenuClickListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MarketMenuHolder holder)) {
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
        switch (raw) {
            case MarketPagedMenu.PREV_SLOT -> {
                if (holder.getPage() > 0) {
                    MarketPagedMenu.render(holder, holder.getPage() - 1);
                }
            }
            case MarketPagedMenu.BACK_SLOT -> {
                Consumer<Player> back = holder.getBackAction();
                if (back != null) {
                    back.accept(player);
                }
            }
            case MarketPagedMenu.CLOSE_SLOT -> player.closeInventory();
            case MarketPagedMenu.NEXT_SLOT -> {
                if (holder.getPage() < holder.getTotalPages() - 1) {
                    MarketPagedMenu.render(holder, holder.getPage() + 1);
                }
            }
            default -> handleContentClick(holder, player, raw, event.isShiftClick());
        }
    }

    private void handleContentClick(MarketMenuHolder holder, Player player, int raw, boolean shiftClick) {
        if (raw < 0 || raw >= MarketPagedMenu.PAGE_SIZE) {
            return;
        }
        int index = holder.getPage() * MarketPagedMenu.PAGE_SIZE + raw;
        List<Consumer<Player>> shiftActions = holder.getShiftClickActions();
        if (shiftClick && index >= 0 && index < shiftActions.size() && shiftActions.get(index) != null) {
            shiftActions.get(index).accept(player);
            return;
        }
        List<Consumer<Player>> actions = holder.getActions();
        if (index >= 0 && index < actions.size() && actions.get(index) != null) {
            actions.get(index).accept(player);
        }
    }
}
