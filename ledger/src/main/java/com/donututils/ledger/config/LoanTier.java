package com.donututils.ledger.config;

public record LoanTier(int minScore, double maxLoanAmount, double aprPercent) {
}
