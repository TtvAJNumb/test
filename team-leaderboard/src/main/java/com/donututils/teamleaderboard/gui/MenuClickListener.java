package com.donututils.teamleaderboard.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class MenuClickListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TeamMenuHolder holder)) {
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
            case PagedMenu.CLOSE_SLOT -> player.closeInventory();
            case PagedMenu.NEXT_SLOT -> {
                if (holder.getPage() < holder.getTotalPages() - 1) {
                    PagedMenu.render(holder, holder.getPage() + 1);
                }
            }
            default -> {
                // Team entries are informational only - nothing to do on click.
            }
        }
    }
}
