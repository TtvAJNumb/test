package com.donututils.donutrep.market.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Holder for a fixed-slot /shop menu (root or one category) - real UDS shop.yml lays every button out
 * at an explicit slot rather than paginating, so this just maps slot -> click action instead of the
 * scrolling-list holder the old auto-generated catalog used. */
public final class MarketMenuHolder implements InventoryHolder {

    private Inventory inventory;
    private final Map<Integer, Consumer<Player>> actions = new HashMap<>();

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void onSlot(int slot, Consumer<Player> action) {
        actions.put(slot, action);
    }

    public void click(Player player, int slot) {
        Consumer<Player> action = actions.get(slot);
        if (action != null) {
            action.accept(player);
        }
    }
}
