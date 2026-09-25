package com.donututils.punishhistory.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.function.Consumer;

public final class MenuClickListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PunishMenuHolder holder)) {
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
            case PagedMenu.PREV_SLOT -> {
                if (holder.getPage() > 0) {
                    PagedMenu.render(holder, holder.getPage() - 1);
                }
            }
            case PagedMenu.BACK_SLOT -> {
                Consumer<Player> back = holder.getBackAction();
                if (back != null) {
                    back.accept(player);
                }
            }
            case PagedMenu.CLOSE_SLOT -> player.closeInventory();
            case PagedMenu.NEXT_SLOT -> {
                if (holder.getPage() < holder.getTotalPages() - 1) {
                    PagedMenu.render(holder, holder.getPage() + 1);
                }
            }
            default -> handleContentClick(holder, player, raw);
        }
    }

    private void handleContentClick(PunishMenuHolder holder, Player player, int raw) {
        if (raw < 0 || raw >= PagedMenu.PAGE_SIZE) {
            return;
        }
        int index = holder.getPage() * PagedMenu.PAGE_SIZE + raw;
        List<Consumer<Player>> actions = holder.getActions();
        if (index >= 0 && index < actions.size() && actions.get(index) != null) {
            actions.get(index).accept(player);
        }
    }
}
