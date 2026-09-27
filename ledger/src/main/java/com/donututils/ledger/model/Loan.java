package com.donututils.ledger.model;

/** One player's loan. v1 supports a single active loan per player, same as this repo's other
 * loan-bearing plugins. Interest compounds continuously (daily rate, prorated); a missed due date
 * counts against the borrower's credit and, past a configured threshold, triggers default. */
public final class Loan {

    public enum Status {
        ACTIVE, PAID, DEFAULTED
    }

    private long id;
    private final double principal;
    private double remainingBalance;
    private final double aprPercent;
    private final int termDays;
    private final long issuedAtMillis;
    private long nextPaymentDueMillis;
    private long lastAccrualMillis;
    private int missedPayments;
    private Status status;

    public Loan(long id, double principal, double aprPercent, int termDays, long issuedAtMillis) {
        this.id = id;
        this.principal = principal;
        this.remainingBalance = principal;
        this.aprPercent = aprPercent;
        this.termDays = termDays;
        this.issuedAtMillis = issuedAtMillis;
        this.lastAccrualMillis = issuedAtMillis;
        this.nextPaymentDueMillis = issuedAtMillis + termDays * 24L * 60 * 60 * 1000;
        this.missedPayments = 0;
        this.status = Status.ACTIVE;
    }

    public long id() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public double principal() {
        return principal;
    }

    public double remainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(double remainingBalance) {
        this.remainingBalance = remainingBalance;
    }

    public double aprPercent() {
        return aprPercent;
    }

    public int termDays() {
        return termDays;
    }

    public long issuedAtMillis() {
        return issuedAtMillis;
    }

    public long nextPaymentDueMillis() {
        return nextPaymentDueMillis;
    }

    public void advanceNextPaymentDue() {
        nextPaymentDueMillis += termDays * 24L * 60 * 60 * 1000;
    }

    public int missedPayments() {
        return missedPayments;
    }

    public void incrementMissedPayments() {
        missedPayments++;
    }

    public Status status() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isPaidOff() {
        return remainingBalance <= 0.005;
    }

    /** Continuously compounds the remaining balance for whatever time has elapsed since the last
     * accrual, at a daily rate derived from the APR (aprPercent/100/365 per day, compounded for the
     * number of days elapsed - NOT days*365, which would apply a full year of growth every tick). */
    public void accrue(long now) {
        long elapsedMillis = now - lastAccrualMillis;
        if (elapsedMillis <= 0) {
            return;
        }
        double days = elapsedMillis / (24.0 * 60 * 60 * 1000);
        remainingBalance *= Math.pow(1 + aprPercent / 100.0 / 365.0, days);
        lastAccrualMillis = now;
    }
}
