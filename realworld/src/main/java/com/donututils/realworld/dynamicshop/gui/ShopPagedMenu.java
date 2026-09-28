package com.donututils.realworld.dynamicshop.gui;

import com.donututils.realworld.dynamicshop.model.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Same 54-slot paginator shape used across this repo's other GUI plugins, extended with a row of
 * currency filter tabs. Classic String-based Bukkit API only; {@code openInventory} is called
 * reflectively for cross-version safety. */
public final class ShopPagedMenu {

    public static final int SIZE = 54;
    public static final int PAGE_SIZE = 45;
    public static final int PREV_SLOT = 45;
    public static final int CLOSE_SLOT = 49;
    public static final int NEXT_SLOT = 53;
    private static final int[] FILTER_SLOTS = {46, 47, 48};

    private ShopPagedMenu() {
    }

    public static void open(Player player, ShopMenuHolder holder, String title, List<ShopItem> allItems,
                             String currentFilter, List<String> availableCurrencyIds, PriceFormatter formatter) {
        Inventory inventory = Bukkit.createInventory(holder, SIZE, legacy(title));
        holder.setInventory(inventory);
        holder.setCurrentFilter(currentFilter);

        List<ShopItem> filtered = new ArrayList<>();
        for (ShopItem item : allItems) {
            if ("ALL".equalsIgnoreCase(currentFilter) || item.currency().equalsIgnoreCase(currentFilter)) {
                filtered.add(item);
            }
        }
        holder.setItems(filtered);

        Map<Integer, String> filterSlots = new LinkedHashMap<>();
        List<String> filters = new ArrayList<>();
        filters.add("ALL");
        filters.addAll(availableCurrencyIds);
        for (int i = 0; i < filters.size() && i < FILTER_SLOTS.length; i++) {
            filterSlots.put(FILTER_SLOTS[i], filters.get(i));
        }
        holder.setFilterSlots(filterSlots);

        render(holder, 0, formatter);
        openInventorySafely(player, inventory);
    }

    public static void render(ShopMenuHolder holder, int page, PriceFormatter formatter) {
        Inventory inventory = holder.getInventory();
        if (inventory == null) {
            return;
        }
        List<ShopItem> items = holder.getItems();
        int totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        inventory.clear();
        int start = page * PAGE_SIZE;
        int end = Math.min(items.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            inventory.setItem(i - start, itemStack(items.get(i), formatter));
        }

        if (page > 0) {
            inventory.setItem(PREV_SLOT, navItem(Material.ARROW, "&ePrevious Page"));
        }
        for (Map.Entry<Integer, String> entry : holder.getFilterSlots().entrySet()) {
            boolean active = entry.getValue().equalsIgnoreCase(holder.getCurrentFilter());
            String label = entry.getValue().equalsIgnoreCase("ALL") ? "All" : capitalize(entry.getValue());
            inventory.setItem(entry.getKey(), navItem(active ? Material.LIME_DYE : Material.GRAY_DYE,
                    (active ? "&a&l" : "&7") + label + " Filter"));
        }
        inventory.setItem(CLOSE_SLOT, navItem(Material.BARRIER, "&cClose"));
        if (page < totalPages - 1) {
            inventory.setItem(NEXT_SLOT, navItem(Material.ARROW, "&eNext Page"));
        }

        holder.setPage(page);
        holder.setTotalPages(totalPages);
    }

    private static ItemStack itemStack(ShopItem shopItem, PriceFormatter formatter) {
        Material material = resolveMaterial(shopItem.material());
        List<String> lore = new ArrayList<>();
        lore.add("&7Category: &f" + shopItem.category());
        lore.add("&7Price: &f" + formatter.format(shopItem.currency(), shopItem.currentPrice()));
        if (shopItem.buyEnabled()) {
            lore.add("&aLeft-click to buy");
        }
        if (shopItem.sellEnabled()) {
            lore.add("&cShift-click to sell");
        }
        if (!shopItem.buyEnabled() && !shopItem.sellEnabled()) {
            lore.add("&8Currently unavailable");
        }
        return item(material, "&f" + shopItem.displayName(), lore);
    }

    private static Material resolveMaterial(String materialName) {
        try {
            return Material.valueOf(materialName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            return Material.PAPER;
        }
    }

    private static String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase(Locale.ROOT);
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

    private static ItemStack navItem(Material material, String name) {
        return item(material, name, List.of());
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(legacy(name));
            List<String> colored = new ArrayList<>();
            for (String line : lore) {
                colored.add(legacy(line));
            }
            meta.setLore(colored);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static String legacy(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
