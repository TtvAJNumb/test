package com.donututils.dynamicshop.model;

/** One player's outstanding loan. v1 supports a single active loan per player - the interest accrues
 * continuously (compounded daily, prorated for partial days) whenever the loan is looked at. */
public final class Loan {

    private final String currency;
    private double principal;
    private final double dailyInterestRatePercent;
    private final long issuedAtMillis;
    private long lastAccrualMillis;

    public Loan(String currency, double principal, double dailyInterestRatePercent, long now) {
        this.currency = currency;
        this.principal = principal;
        this.dailyInterestRatePercent = dailyInterestRatePercent;
        this.issuedAtMillis = now;
        this.lastAccrualMillis = now;
    }

    public String currency() {
        return currency;
    }

    public double principal() {
        return principal;
    }

    public void setPrincipal(double principal) {
        this.principal = principal;
    }

    public double dailyInterestRatePercent() {
        return dailyInterestRatePercent;
    }

    public long issuedAtMillis() {
        return issuedAtMillis;
    }

    public long lastAccrualMillis() {
        return lastAccrualMillis;
    }

    public void setLastAccrualMillis(long lastAccrualMillis) {
        this.lastAccrualMillis = lastAccrualMillis;
    }

    public void accrue(long now) {
        long elapsedMillis = now - lastAccrualMillis;
        if (elapsedMillis <= 0) {
            return;
        }
        double days = elapsedMillis / (24.0 * 60 * 60 * 1000);
        principal *= Math.pow(1 + dailyInterestRatePercent / 100.0, days);
        lastAccrualMillis = now;
    }

    public boolean isPaidOff() {
        return principal <= 0.005;
    }
}
