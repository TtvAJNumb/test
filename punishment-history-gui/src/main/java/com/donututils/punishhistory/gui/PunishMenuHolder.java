package com.donututils.punishhistory.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Consumer;

/** Marker holder for both the roster and the per-player detail menu. */
public final class PunishMenuHolder implements InventoryHolder {

    private Inventory inventory;
    private List<ItemStack> items = List.of();
    private List<Consumer<Player>> actions = List.of();
    private Consumer<Player> backAction;
    private int page;
    private int totalPages = 1;

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

    public Consumer<Player> getBackAction() {
        return backAction;
    }

    public void setBackAction(Consumer<Player> backAction) {
        this.backAction = backAction;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
