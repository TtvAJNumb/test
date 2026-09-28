package com.donututils.realworld.dynamicshop.service;

import com.donututils.realworld.dynamicshop.config.DynamicShopConfig;
import com.donututils.realworld.dynamicshop.currency.CurrencyProvider;
import com.donututils.realworld.dynamicshop.currency.CurrencyRegistry;
import com.donututils.realworld.dynamicshop.engine.ShopItemRegistry;
import com.donututils.realworld.dynamicshop.gui.ShopMenuHolder;
import com.donututils.realworld.dynamicshop.gui.ShopPagedMenu;
import com.donututils.realworld.dynamicshop.listener.ChatQuantityPrompt;
import com.donututils.realworld.dynamicshop.model.ShopItem;
import com.donututils.realworld.dynamicshop.util.InventoryUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/** Owns the shop GUI: opening it, the currency filter, and the buy/sell flows including actually
 * giving/removing real inventory items (TradingService only moves currency and nudges price). */
public final class ShopGuiService {

    private final ShopItemRegistry itemRegistry;
    private final CurrencyRegistry currencyRegistry;
    private final TradingService tradingService;
    private final ChatQuantityPrompt quantityPrompt;
    private final Supplier<DynamicShopConfig> configSupplier;

    public ShopGuiService(ShopItemRegistry itemRegistry, CurrencyRegistry currencyRegistry, TradingService tradingService,
                           ChatQuantityPrompt quantityPrompt, Supplier<DynamicShopConfig> configSupplier) {
        this.itemRegistry = itemRegistry;
        this.currencyRegistry = currencyRegistry;
        this.tradingService = tradingService;
        this.quantityPrompt = quantityPrompt;
        this.configSupplier = configSupplier;
    }

    public void open(Player player, String filter) {
        ShopMenuHolder holder = new ShopMenuHolder();
        holder.setOnBuy(this::promptBuy);
        holder.setOnSell(this::promptSell);
        holder.setOnFilterSelected(this::open);

        List<ShopItem> items = new ArrayList<>(itemRegistry.all());
        items.sort((a, b) -> a.material().compareTo(b.material()));

        List<String> availableCurrencies = new ArrayList<>();
        for (CurrencyProvider provider : currencyRegistry.all()) {
            availableCurrencies.add(provider.id());
        }

        ShopPagedMenu.open(player, holder, configSupplier.get().guiTitle(), items, filter, availableCurrencies, this::formatPrice);
    }

    private String formatPrice(String currencyId, double amount) {
        CurrencyProvider provider = currencyRegistry.get(currencyId);
        return provider == null ? String.format(Locale.US, "%,.2f %s", amount, currencyId) : provider.format(amount);
    }

    private void promptBuy(Player player, ShopItem item) {
        player.closeInventory();
        if (!item.buyEnabled()) {
            player.sendMessage(legacy("&c" + item.displayName() + " isn't currently buyable."));
            return;
        }
        quantityPrompt.prompt(player, "&aHow many " + item.displayName() + " do you want to buy?", qty -> {
            if (qty <= 0) {
                player.sendMessage(legacy("&cEnter a positive number."));
                return;
            }
            CurrencyProvider currency = currencyRegistry.get(item.currency());
            double total = item.currentPrice() * qty;
            String preview = "&aBuying " + qty + "x " + item.displayName() + " for &f"
                    + (currency == null ? String.valueOf(total) : currency.format(total)) + "&a.";
            quantityPrompt.promptConfirmation(player, preview, () -> completeBuy(player, item, qty));
        });
    }

    private void completeBuy(Player player, ShopItem item, long qty) {
        TradingService.TradeResult result = tradingService.buy(player.getUniqueId(), item.material(), qty);
        if (result.success()) {
            giveItems(player, item.material(), qty);
        }
        player.sendMessage(legacy((result.success() ? "&a" : "&c") + result.message()));
    }

    private void giveItems(Player player, String material, long qty) {
        Material type = resolveMaterial(material);
        long remaining = qty;
        while (remaining > 0) {
            int batch = (int) Math.min(remaining, Integer.MAX_VALUE);
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(new ItemStack(type, batch));
            remaining = 0;
            if (!overflow.isEmpty()) {
                for (ItemStack leftover : overflow.values()) {
                    player.getWorld().dropItem(player.getLocation(), leftover);
                }
                player.sendMessage(legacy("&eYour inventory was full - some items were dropped at your feet."));
            }
        }
    }

    private void promptSell(Player player, ShopItem item) {
        player.closeInventory();
        if (!item.sellEnabled()) {
            player.sendMessage(legacy("&c" + item.displayName() + " isn't currently sellable."));
            return;
        }
        quantityPrompt.prompt(player, "&cHow many " + item.displayName() + " do you want to sell?", qty -> {
            if (qty <= 0) {
                player.sendMessage(legacy("&cEnter a positive number."));
                return;
            }
            Material type = resolveMaterial(item.material());
            long owned = InventoryUtil.count(player.getInventory(), type);
            if (owned < qty) {
                player.sendMessage(legacy("&cYou only have " + owned + "x " + item.displayName() + "."));
                return;
            }
            CurrencyProvider currency = currencyRegistry.get(item.currency());
            double total = item.currentPrice() * qty;
            String preview = "&cSelling " + qty + "x " + item.displayName() + " for &f"
                    + (currency == null ? String.valueOf(total) : currency.format(total)) + "&c.";
            quantityPrompt.promptConfirmation(player, preview, () -> completeSell(player, item, qty));
        });
    }

    private void completeSell(Player player, ShopItem item, long qty) {
        Material type = resolveMaterial(item.material());
        long owned = InventoryUtil.count(player.getInventory(), type);
        if (owned < qty) {
            player.sendMessage(legacy("&cYou only have " + owned + "x " + item.displayName() + " now."));
            return;
        }
        InventoryUtil.remove(player.getInventory(), type, qty);
        TradingService.TradeResult result = tradingService.sell(player.getUniqueId(), item.material(), qty);
        player.sendMessage(legacy((result.success() ? "&a" : "&c") + result.message()));
    }

    private static Material resolveMaterial(String materialName) {
        try {
            return Material.valueOf(materialName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            return Material.STONE;
        }
    }

    private static String legacy(String text) {
        return ShopPagedMenu.legacy(text);
    }
}
