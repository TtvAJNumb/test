package com.donututils.realworld.stockmarket.engine;

import com.donututils.realworld.stockmarket.model.Stock;
import com.donututils.realworld.stockmarket.storage.StockStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Holds every stock's live state in memory; the source of truth while the server is running. */
public final class StockRegistry {

    private final StockStore store;
    private final Map<String, Stock> stocks;

    public StockRegistry(StockStore store) {
        this.store = store;
        this.stocks = new LinkedHashMap<>(store.load());
    }

    public Stock get(String symbol) {
        return symbol == null ? null : stocks.get(symbol.toUpperCase());
    }

    public boolean exists(String symbol) {
        return get(symbol) != null;
    }

    public void add(Stock stock) {
        stocks.put(stock.symbol(), stock);
    }

    public void remove(String symbol) {
        stocks.remove(symbol.toUpperCase());
    }

    public Collection<Stock> all() {
        return stocks.values();
    }

    public List<Stock> allActive() {
        List<Stock> active = new ArrayList<>();
        for (Stock stock : stocks.values()) {
            if (!stock.delisted()) {
                active.add(stock);
            }
        }
        return active;
    }

    public void saveAll() {
        store.save(stocks);
    }
}
