package com.donututils.realworld.stockmarket.model;

/** One player's position in one stock. costBasisTotal is the total amount paid for all held shares. */
public record Holding(long shares, double costBasisTotal) {

    public static final Holding NONE = new Holding(0, 0);

    public double averageCost() {
        return shares == 0 ? 0 : costBasisTotal / shares;
    }

    public Holding withBuy(long addedShares, double cost) {
        return new Holding(shares + addedShares, costBasisTotal + cost);
    }

    /** Reduces shares by soldShares, scaling down the retained cost basis proportionally (FIFO-average method). */
    public Holding withSell(long soldShares) {
        if (soldShares >= shares) {
            return NONE;
        }
        double remainingFraction = (double) (shares - soldShares) / shares;
        return new Holding(shares - soldShares, costBasisTotal * remainingFraction);
    }

    public boolean isEmpty() {
        return shares <= 0;
    }
}
