package com.donututils.realworld.dynamicshop.model;

import java.util.Locale;

/** A single tradeable item: which currency it's priced in, its current supply/demand-adjusted
 * price, and its per-item exploit-protection limits. Mirrors StockMarket's Stock model, but for a
 * real in-game item material instead of an abstract company. */
public final class ShopItem {

    private final String material;
    private String currency;
    private String category;
    private String displayName;
    private double basePrice;
    private double currentPrice;
    private double minPrice;
    private double maxPrice;
    private double volatility;
    private boolean buyEnabled;
    private boolean sellEnabled;
    private int maxBuyPerTransaction;
    private int maxSellPerTransaction;

    private transient double pendingNetFlow;

    public ShopItem(String material, String currency) {
        this.material = material.toUpperCase(Locale.ROOT);
        this.currency = currency;
        this.category = "General";
        this.displayName = this.material;
        this.basePrice = 1.0;
        this.currentPrice = 1.0;
        this.minPrice = 0.1;
        this.maxPrice = 10.0;
        this.volatility = 0.01;
        this.buyEnabled = true;
        this.sellEnabled = true;
        this.maxBuyPerTransaction = 64;
        this.maxSellPerTransaction = 64;
    }

    public String material() {
        return material;
    }

    public String currency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String category() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category == null || category.isBlank() ? "General" : category;
    }

    public String displayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName == null || displayName.isBlank() ? material : displayName;
    }

    public double basePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public double currentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double minPrice() {
        return minPrice;
    }

    public void setMinPrice(double minPrice) {
        this.minPrice = minPrice;
    }

    public double maxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public double volatility() {
        return volatility;
    }

    public void setVolatility(double volatility) {
        this.volatility = volatility;
    }

    public boolean buyEnabled() {
        return buyEnabled;
    }

    public void setBuyEnabled(boolean buyEnabled) {
        this.buyEnabled = buyEnabled;
    }

    public boolean sellEnabled() {
        return sellEnabled;
    }

    public void setSellEnabled(boolean sellEnabled) {
        this.sellEnabled = sellEnabled;
    }

    public int maxBuyPerTransaction() {
        return maxBuyPerTransaction;
    }

    public void setMaxBuyPerTransaction(int maxBuyPerTransaction) {
        this.maxBuyPerTransaction = maxBuyPerTransaction;
    }

    public int maxSellPerTransaction() {
        return maxSellPerTransaction;
    }

    public void setMaxSellPerTransaction(int maxSellPerTransaction) {
        this.maxSellPerTransaction = maxSellPerTransaction;
    }

    /** Called on every buy (positive notional) or sell (negative notional) to nudge next tick's price. */
    public void addNetFlow(double signedNotional) {
        this.pendingNetFlow += signedNotional;
    }

    public double takePendingNetFlow() {
        double flow = pendingNetFlow;
        pendingNetFlow = 0;
        return flow;
    }
}
