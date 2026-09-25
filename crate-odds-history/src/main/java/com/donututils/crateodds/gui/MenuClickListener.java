package com.donututils.crateodds.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class MenuClickListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CrateMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) {
            return; // ignore clicks in the player's own inventory while our menu is open
        }

        switch (event.getRawSlot()) {
            case PagedMenu.PREV_SLOT -> {
                if (holder.getPage() > 0) {
                    PagedMenu.render(holder, holder.getPage() - 1);
                }
            }
            case PagedMenu.CLOSE_SLOT -> player.closeInventory();
            case PagedMenu.NEXT_SLOT -> {
                if (holder.getPage() < holder.getTotalPages() - 1) {
                    PagedMenu.render(holder, holder.getPage() + 1);
                }
            }
            default -> {
                // display-only items elsewhere in the menu
            }
        }
    }
}
