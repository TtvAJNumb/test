package com.donututils.donutrep.staff.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Consumer;

/** Generic action-per-slot holder for the roster and inspection menus - same pattern as this
 * project's other subsystem-owned GUI plumbing (Careers' onboarding menus, Market's paged menu). */
public final class StaffMenuHolder implements InventoryHolder {

    private Inventory inventory;
    private List<ItemStack> items = List.of();
    private List<Consumer<Player>> actions = List.of();

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public void setItems(List<ItemStack> items) {
        this.items = items;
    }

    public List<Consumer<Player>> getActions() {
        return actions;
    }

    public void setActions(List<Consumer<Player>> actions) {
        this.actions = actions;
    }
}
