package com.donututils.stockmarket.model;

/**
 * Live, mutable state for one stock. Held entirely in memory while the plugin runs; persisted to
 * stocks.yml on save. Price movement (see engine.PriceEngine) is a geometric-Brownian-motion random
 * walk (drift + volatility) plus a small, capped nudge from real buy/sell pressure accumulated in
 * {@link #pendingNetFlow} since the last tick - real trading visibly moves the price without any
 * single trade being able to dominate it.
 */
public final class Stock {

    private final String symbol;
    private String name;
    private String sector;

    private double price;
    private double previousClose;
    private double dayOpen;
    private double dayHigh;
    private double dayLow;
    private long volumeToday;
    private long sharesOutstanding;

    private double drift;
    private double volatility;

    private double dividendYieldPerPayout;
    private long dividendIntervalMillis;
    private long lastDividendAtMillis;

    private boolean halted;
    private long haltedUntilMillis;
    private boolean delisted;

    private double eventDriftBoost;
    private double eventVolatilityBoost;
    private long eventUntilMillis;

    private transient double pendingNetFlow;

    public Stock(String symbol, String name, String sector, double startingPrice, long sharesOutstanding,
                 double drift, double volatility) {
        this.symbol = symbol.toUpperCase();
        this.name = name;
        this.sector = sector;
        this.price = startingPrice;
        this.previousClose = startingPrice;
        this.dayOpen = startingPrice;
        this.dayHigh = startingPrice;
        this.dayLow = startingPrice;
        this.sharesOutstanding = sharesOutstanding;
        this.drift = drift;
        this.volatility = volatility;
    }

    public String symbol() { return symbol; }

    public String name() { return name; }
    public void setName(String name) { this.name = name; }

    public String sector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public double price() { return price; }
    public void setPrice(double price) { this.price = Math.max(0.01, price); }

    public double previousClose() { return previousClose; }
    public void setPreviousClose(double v) { this.previousClose = v; }

    public double dayOpen() { return dayOpen; }
    public void setDayOpen(double v) { this.dayOpen = v; }

    public double dayHigh() { return dayHigh; }
    public void setDayHigh(double v) { this.dayHigh = v; }

    public double dayLow() { return dayLow; }
    public void setDayLow(double v) { this.dayLow = v; }

    public long volumeToday() { return volumeToday; }
    public void addVolume(long shares) { this.volumeToday += shares; }
    public void resetVolume() { this.volumeToday = 0; }

    public long sharesOutstanding() { return sharesOutstanding; }
    public void setSharesOutstanding(long v) { this.sharesOutstanding = v; }

    public double marketCap() { return price * sharesOutstanding; }

    public double drift() { return drift; }
    public void setDrift(double v) { this.drift = v; }

    public double volatility() { return volatility; }
    public void setVolatility(double v) { this.volatility = v; }

    public double dividendYieldPerPayout() { return dividendYieldPerPayout; }
    public void setDividendYieldPerPayout(double v) { this.dividendYieldPerPayout = v; }

    public long dividendIntervalMillis() { return dividendIntervalMillis; }
    public void setDividendIntervalMillis(long v) { this.dividendIntervalMillis = v; }

    public boolean paysDividends() { return dividendYieldPerPayout > 0 && dividendIntervalMillis > 0; }

    public long lastDividendAtMillis() { return lastDividendAtMillis; }
    public void setLastDividendAtMillis(long v) { this.lastDividendAtMillis = v; }

    public boolean halted() { return halted; }

    public boolean haltActive(long nowMillis) {
        if (!halted) {
            return false;
        }
        if (nowMillis >= haltedUntilMillis) {
            halted = false;
            return false;
        }
        return true;
    }

    public void halt(long untilMillis) {
        this.halted = true;
        this.haltedUntilMillis = untilMillis;
    }

    public long haltedUntilMillis() { return haltedUntilMillis; }

    public boolean delisted() { return delisted; }
    public void setDelisted(boolean v) { this.delisted = v; }

    public void applyEvent(double driftBoost, double volatilityBoost, long untilMillis) {
        this.eventDriftBoost = driftBoost;
        this.eventVolatilityBoost = volatilityBoost;
        this.eventUntilMillis = untilMillis;
    }

    public boolean hasActiveEvent(long nowMillis) {
        if (eventUntilMillis == 0) {
            return false;
        }
        if (nowMillis >= eventUntilMillis) {
            eventDriftBoost = 0;
            eventVolatilityBoost = 0;
            eventUntilMillis = 0;
            return false;
        }
        return true;
    }

    public double effectiveDrift() { return drift + eventDriftBoost; }
    public double effectiveVolatility() { return Math.max(0.0001, volatility + eventVolatilityBoost); }

    public double eventDriftBoost() { return eventDriftBoost; }
    public double eventVolatilityBoost() { return eventVolatilityBoost; }
    public long eventUntilMillis() { return eventUntilMillis; }

    /** Called by the trading service on every buy (positive) or sell (negative) fill. */
    public void addNetFlow(double signedNotional) {
        this.pendingNetFlow += signedNotional;
    }

    public double takePendingNetFlow() {
        double flow = pendingNetFlow;
        pendingNetFlow = 0;
        return flow;
    }
}
