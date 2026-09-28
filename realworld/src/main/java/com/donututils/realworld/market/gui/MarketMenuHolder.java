package com.donututils.realworld.market.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Consumer;

/** Marker holder shared by every Market menu (root, dimension, section, boutique). Same shape as
 * StockMarket's own menu holder - kept as a separate class per subsystem rather than shared, matching
 * this project's convention of each subsystem owning its own GUI plumbing. */
public final class MarketMenuHolder implements InventoryHolder {

    private Inventory inventory;
    private List<ItemStack> items = List.of();
    private List<Consumer<Player>> actions = List.of();
    private List<Consumer<Player>> shiftClickActions = List.of();
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

    /** Optional per-slot shift-click action (e.g. "sell" where the plain click is "buy"). Empty list
     * (the default) means every slot just falls back to its normal click action. */
    public List<Consumer<Player>> getShiftClickActions() {
        return shiftClickActions;
    }

    public void setShiftClickActions(List<Consumer<Player>> shiftClickActions) {
        this.shiftClickActions = shiftClickActions;
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
