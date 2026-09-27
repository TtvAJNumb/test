package com.donututils.dynamicshop.util;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** Small helpers for counting/removing a material from a player's inventory before crediting a sale -
 * selling must never pay out for items the player doesn't actually have. */
public final class InventoryUtil {

    private InventoryUtil() {
    }

    public static long count(Inventory inventory, Material material) {
        long total = 0;
        ItemStack[] contents = inventory.getContents();
        if (contents == null) {
            return 0;
        }
        for (ItemStack stack : contents) {
            if (stack != null && stack.getType() == material) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    /** Removes up to {@code amount} of the material, returning how many were actually removed. */
    public static long remove(Inventory inventory, Material material, long amount) {
        long remaining = amount;
        ItemStack[] contents = inventory.getContents();
        if (contents == null) {
            return 0;
        }
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != material) {
                continue;
            }
            int stackAmount = stack.getAmount();
            if (stackAmount <= remaining) {
                remaining -= stackAmount;
                inventory.setItem(i, null);
            } else {
                stack.setAmount(stackAmount - (int) remaining);
                inventory.setItem(i, stack);
                remaining = 0;
            }
        }
        return amount - remaining;
    }
}
