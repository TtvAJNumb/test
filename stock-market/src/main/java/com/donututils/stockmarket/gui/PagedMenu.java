package com.donututils.stockmarket.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;

/**
 * Same paginator shape used across this project's other GUI plugins: rows 1-5 content, row 6
 * controls. Classic String-based Bukkit API only, and {@code openInventory} is called reflectively,
 * for cross-version safety (return type varies across Bukkit/Paper builds).
 */
public final class PagedMenu {

    public static final int PAGE_SIZE = 45;
    public static final int SIZE = 54;
    public static final int PREV_SLOT = 45;
    public static final int BACK_SLOT = 46;
    public static final int CLOSE_SLOT = 49;
    public static final int NEXT_SLOT = 53;

    private PagedMenu() {
    }

    public static void open(Player player, StockMenuHolder holder, String title, List<ItemStack> items,
                             List<Consumer<Player>> actions, Consumer<Player> backAction) {
        Inventory inventory = Bukkit.createInventory(holder, SIZE, legacy(title));
        holder.setInventory(inventory);
        holder.setItems(items);
        holder.setActions(actions);
        holder.setBackAction(backAction);
        render(holder, 0);
        openInventorySafely(player, inventory);
    }

    private static void openInventorySafely(Player player, Inventory inventory) {
        try {
            Method method = player.getClass().getMethod("openInventory", Inventory.class);
            method.invoke(player, inventory);
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            throw new IllegalStateException("Could not find a way to open an inventory on this server: " + ex, ex);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new IllegalStateException("Opening the menu inventory failed: " + cause, cause);
        }
    }

    public static void render(StockMenuHolder holder, int page) {
        Inventory inventory = holder.getInventory();
        if (inventory == null) {
            return;
        }
        List<ItemStack> items = holder.getItems();
        int totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        inventory.clear();
        int start = page * PAGE_SIZE;
        int end = Math.min(items.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            inventory.setItem(i - start, items.get(i));
        }

        if (page > 0) {
            inventory.setItem(PREV_SLOT, navItem(Material.ARROW, "&ePrevious Page"));
        }
        if (holder.getBackAction() != null) {
            inventory.setItem(BACK_SLOT, navItem(Material.SPECTRAL_ARROW, "&aBack"));
        }
        inventory.setItem(CLOSE_SLOT, navItem(Material.BARRIER, "&cClose"));
        if (page < totalPages - 1) {
            inventory.setItem(NEXT_SLOT, navItem(Material.ARROW, "&eNext Page"));
        }

        holder.setPage(page);
        holder.setTotalPages(totalPages);
    }

    private static ItemStack navItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(legacy(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static String legacy(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
