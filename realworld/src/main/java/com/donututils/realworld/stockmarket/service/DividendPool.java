package com.donututils.realworld.stockmarket.service;

/**
 * Real money collected from broker fees on trades, which dividends are funded from instead of being
 * deposited into existence. Global across all stocks - broker fees aren't stock-specific revenue,
 * they're the exchange's own commission income funding total shareholder payouts. This closes the
 * loop that previously let dividends create money with nothing backing it: every dollar paid out as
 * a dividend now has to have first come in as a real fee paid by a real trade.
 */
public final class DividendPool {

    private double balance;

    public synchronized void credit(double amount) {
        if (amount > 0) {
            balance += amount;
        }
    }

    public synchronized double balance() {
        return balance;
    }

    /** Withdraws up to {@code amount} from the pool, returning how much was actually available -
     * callers must scale their payout down to whatever this returns, never pay out more. */
    public synchronized double debit(double amount) {
        double taken = Math.min(balance, amount);
        balance -= taken;
        return taken;
    }
}
