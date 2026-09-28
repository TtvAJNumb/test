package com.donututils.realworld.ledger.model;

/** A player's credit standing. Score is clamped to the real-world 300-850 range. */
public final class CreditProfile {

    public static final int MIN_SCORE = 300;
    public static final int MAX_SCORE = 850;

    private int score;
    private int onTimePayments;
    private int missedPayments;
    private int defaults;

    public CreditProfile(int score, int onTimePayments, int missedPayments, int defaults) {
        this.score = clamp(score);
        this.onTimePayments = onTimePayments;
        this.missedPayments = missedPayments;
        this.defaults = defaults;
    }

    public int score() {
        return score;
    }

    public int onTimePayments() {
        return onTimePayments;
    }

    public int missedPayments() {
        return missedPayments;
    }

    public int defaults() {
        return defaults;
    }

    public void recordOnTimePayment(int points) {
        onTimePayments++;
        score = clamp(score + points);
    }

    public void recordMissedPayment(int points) {
        missedPayments++;
        score = clamp(score + points);
    }

    public void recordDefault(int points) {
        defaults++;
        score = clamp(score + points);
    }

    private static int clamp(int value) {
        return Math.max(MIN_SCORE, Math.min(MAX_SCORE, value));
    }
}
