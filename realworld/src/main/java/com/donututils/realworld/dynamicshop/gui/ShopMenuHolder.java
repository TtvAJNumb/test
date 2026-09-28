package com.donututils.realworld.dynamicshop.gui;

import com.donututils.realworld.dynamicshop.model.ShopItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/** Holder for the shop's single browse screen: a page of items plus currency filter tabs. */
public final class ShopMenuHolder implements InventoryHolder {

    private Inventory inventory;
    private List<ShopItem> items = List.of();
    private String currentFilter = "ALL";
    private Map<Integer, String> filterSlots = Map.of();
    private BiConsumer<Player, ShopItem> onBuy;
    private BiConsumer<Player, ShopItem> onSell;
    private BiConsumer<Player, String> onFilterSelected;
    private int page;
    private int totalPages = 1;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public List<ShopItem> getItems() {
        return items;
    }

    public void setItems(List<ShopItem> items) {
        this.items = items;
    }

    public String getCurrentFilter() {
        return currentFilter;
    }

    public void setCurrentFilter(String currentFilter) {
        this.currentFilter = currentFilter;
    }

    public Map<Integer, String> getFilterSlots() {
        return filterSlots;
    }

    public void setFilterSlots(Map<Integer, String> filterSlots) {
        this.filterSlots = filterSlots;
    }

    public BiConsumer<Player, ShopItem> getOnBuy() {
        return onBuy;
    }

    public void setOnBuy(BiConsumer<Player, ShopItem> onBuy) {
        this.onBuy = onBuy;
    }

    public BiConsumer<Player, ShopItem> getOnSell() {
        return onSell;
    }

    public void setOnSell(BiConsumer<Player, ShopItem> onSell) {
        this.onSell = onSell;
    }

    public BiConsumer<Player, String> getOnFilterSelected() {
        return onFilterSelected;
    }

    public void setOnFilterSelected(BiConsumer<Player, String> onFilterSelected) {
        this.onFilterSelected = onFilterSelected;
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
