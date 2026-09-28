package com.donututils.realworld.ledger.config;

public record LoanTier(int minScore, double maxLoanAmount, double aprPercent) {
}
