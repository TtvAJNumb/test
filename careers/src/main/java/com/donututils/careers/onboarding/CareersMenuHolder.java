package com.donututils.careers.onboarding;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/** Generic GUI holder for both the age-tier and job pickers - each clickable slot maps to a
 * Runnable, so both menus share one InventoryClickEvent listener instead of needing a holder
 * subclass per screen. */
public final class CareersMenuHolder implements InventoryHolder {

    private Inventory inventory;
    private final Map<Integer, Runnable> actions = new HashMap<>();

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public void onSlot(int slot, Runnable action) {
        actions.put(slot, action);
    }

    public Runnable actionFor(int slot) {
        return actions.get(slot);
    }
}
