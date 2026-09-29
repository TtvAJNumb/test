package com.donututils.realworld.staff.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.function.Consumer;

public final class StaffMenuClickListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof StaffMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        List<Consumer<Player>> actions = holder.getActions();
        if (slot >= 0 && slot < actions.size() && actions.get(slot) != null) {
            actions.get(slot).accept(player);
        }
    }
}
