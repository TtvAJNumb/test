package com.donututils.crateodds.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/** Small shared paginator: rows 1-5 hold content, row 6 holds prev/close/next. */
public final class PagedMenu {

    public static final int PAGE_SIZE = 45;
    public static final int SIZE = 54;
    public static final int PREV_SLOT = 45;
    public static final int CLOSE_SLOT = 49;
    public static final int NEXT_SLOT = 53;

    private PagedMenu() {
    }

    public static void open(Player player, CrateMenuHolder holder, String title, List<ItemStack> items) {
        Inventory inventory = Bukkit.createInventory(holder, SIZE, legacy(title));
        holder.setInventory(inventory);
        holder.setItems(items);
        render(holder, 0);
        player.openInventory(inventory);
    }

    public static void render(CrateMenuHolder holder, int page) {
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
            meta.displayName(legacy(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
