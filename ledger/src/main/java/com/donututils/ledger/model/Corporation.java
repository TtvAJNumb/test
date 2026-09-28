package com.donututils.ledger.model;

import java.util.UUID;

/** A player-founded corporation: its share price drifts on its own (like a StockMarket-style random
 * walk) and additionally reacts to real trade pressure from buy/sell orders and to owner-reported
 * profit/loss. */
public final class Corporation {

    private long id;
    private final String name;
    private final String ticker;
    private final UUID founderId;
    private double treasuryBalance;
    private final int totalShares;
    private double sharePrice;
    private String sector;
    private final long foundedAtMillis;
    private boolean trustProtected;

    private transient double pendingNetFlow;
    private transient long lastReportedAtMillis;

    public Corporation(long id, String name, String ticker, UUID founderId, int totalShares,
                        double sharePrice, String sector, long foundedAtMillis) {
        this.id = id;
        this.name = name;
        this.ticker = ticker;
        this.founderId = founderId;
        this.totalShares = totalShares;
        this.sharePrice = sharePrice;
        this.sector = sector;
        this.foundedAtMillis = foundedAtMillis;
        this.treasuryBalance = 0;
        this.trustProtected = false;
    }

    public long id() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String name() {
        return name;
    }

    public String ticker() {
        return ticker;
    }

    public UUID founderId() {
        return founderId;
    }

    public double treasuryBalance() {
        return treasuryBalance;
    }

    public void setTreasuryBalance(double treasuryBalance) {
        this.treasuryBalance = treasuryBalance;
    }

    public int totalShares() {
        return totalShares;
    }

    public double sharePrice() {
        return sharePrice;
    }

    public void setSharePrice(double sharePrice) {
        this.sharePrice = Math.max(0.01, sharePrice);
    }

    public double marketCap() {
        return sharePrice * totalShares;
    }

    public String sector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public long foundedAtMillis() {
        return foundedAtMillis;
    }

    public boolean trustProtected() {
        return trustProtected;
    }

    public void setTrustProtected(boolean trustProtected) {
        this.trustProtected = trustProtected;
    }

    public void addNetFlow(double signedNotional) {
        this.pendingNetFlow += signedNotional;
    }

    public double takePendingNetFlow() {
        double flow = pendingNetFlow;
        pendingNetFlow = 0;
        return flow;
    }

    public long lastReportedAtMillis() {
        return lastReportedAtMillis;
    }

    public void setLastReportedAtMillis(long lastReportedAtMillis) {
        this.lastReportedAtMillis = lastReportedAtMillis;
    }
}
