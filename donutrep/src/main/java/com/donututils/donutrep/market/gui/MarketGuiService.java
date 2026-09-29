package com.donututils.donutrep.market.gui;

import com.donututils.donutrep.market.config.MarketConfig;
import com.donututils.donutrep.market.config.ShopCategory;
import com.donututils.donutrep.market.config.ShopItem;
import com.donututils.donutrep.market.config.ShopMenu;
import com.donututils.donutrep.market.service.MarketService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** Builds the /shop menu tree to match real UDS shop.yml: a fixed 27-slot root with one button per
 * enabled category (END/NETHER/GEAR/FOOD/SHARD/CRATE-KEYS, at their real slots), each opening its own
 * fixed 27-slot item menu with a back button at slot 18. */
public final class MarketGuiService {

    private static final int MENU_SIZE = 27;
    private static final int BACK_SLOT = 18;

    private final MarketService marketService;
    private final Supplier<MarketConfig> configSupplier;

    public MarketGuiService(MarketService marketService, Supplier<MarketConfig> configSupplier) {
        this.marketService = marketService;
        this.configSupplier = configSupplier;
    }

    public void openRoot(Player player) {
        MarketConfig config = configSupplier.get();
        MarketMenuHolder holder = new MarketMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, MENU_SIZE, legacy(config.rootTitle()));
        holder.setInventory(inventory);

        for (ShopCategory category : config.categories()) {
            inventory.setItem(category.slot(), icon(category.icon(), category.displayName(), List.of("&fclick to view the " + plainName(category.id()) + " shop")));
            holder.onSlot(category.slot(), p -> openCategory(p, category.id()));
        }

        openInventorySafely(player, inventory);
    }

    private void openCategory(Player player, String categoryId) {
        MarketConfig config = configSupplier.get();
        ShopMenu menu = config.menus().get(categoryId);
        if (menu == null) {
            player.sendMessage(legacy("&cThat shop category isn't configured."));
            return;
        }
        MarketMenuHolder holder = new MarketMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, MENU_SIZE, legacy(menu.title()));
        holder.setInventory(inventory);

        for (ShopItem item : menu.items()) {
            String priceLine = item.currency() == com.donututils.donutrep.market.config.Currency.MONEY
                    ? String.format(Locale.US, "&fbuy price: &a$%,.2f", item.price())
                    : String.format(Locale.US, "&fbuy price: &d%,.0f Shards", item.price());
            inventory.setItem(item.slot(), icon(item.material(), item.displayName(), List.of(priceLine, "&eclick to buy 1")));
            holder.onSlot(item.slot(), p -> {
                MarketService.TradeResult result = marketService.buy(p, item, 1);
                p.sendMessage(legacy((result.success() ? "&a" : "&c") + result.message()));
                if (result.success()) {
                    openCategory(p, categoryId);
                }
            });
        }

        inventory.setItem(BACK_SLOT, icon(Material.RED_STAINED_GLASS_PANE, "&cback", List.of("&fclick to return")));
        holder.onSlot(BACK_SLOT, this::openRoot);

        openInventorySafely(player, inventory);
    }

    private static String plainName(String categoryId) {
        return categoryId.replace('_', ' ');
    }

    private static ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(legacy(name));
            meta.setLore(lore.stream().map(MarketGuiService::legacy).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void openInventorySafely(Player player, Inventory inventory) {
        try {
            java.lang.reflect.Method method = player.getClass().getMethod("openInventory", Inventory.class);
            method.invoke(player, inventory);
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            throw new IllegalStateException("Could not find a way to open an inventory on this server: " + ex, ex);
        } catch (java.lang.reflect.InvocationTargetException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new IllegalStateException("Opening the menu inventory failed: " + cause, cause);
        }
    }

    private static String legacy(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
