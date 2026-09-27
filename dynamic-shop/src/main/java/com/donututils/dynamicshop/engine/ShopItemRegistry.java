package com.donututils.dynamicshop.engine;

import com.donututils.dynamicshop.model.ShopItem;
import com.donututils.dynamicshop.storage.ShopItemStore;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory view over ShopItemStore, same shape as StockMarket's StockRegistry. */
public final class ShopItemRegistry {

    private final ShopItemStore store;
    private final Map<String, ShopItem> items;

    public ShopItemRegistry(ShopItemStore store) {
        this.store = store;
        this.items = new ConcurrentHashMap<>(store.load());
    }

    public ShopItem get(String material) {
        return items.get(material.toUpperCase(Locale.ROOT));
    }

    public boolean exists(String material) {
        return items.containsKey(material.toUpperCase(Locale.ROOT));
    }

    public void add(ShopItem item) {
        items.put(item.material(), item);
    }

    public void remove(String material) {
        items.remove(material.toUpperCase(Locale.ROOT));
    }

    public Collection<ShopItem> all() {
        return items.values();
    }

    public void saveAll() {
        store.save(items);
    }
}
