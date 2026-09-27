package com.donututils.ledger.config;

import java.util.List;

/** Immutable snapshot of config.yml, re-read on reload. */
public record LedgerConfig(
        double startingBalance,

        double savingsApyPercent,
        int interestTickMinutes,

        int creditStartingScore,
        int pointsPerOnTimePayment,
        int pointsPerMissedPayment,
        int pointsPerDefault,

        List<LoanTier> loanTiers,
        int loanTermDays,
        int missedPaymentsBeforeDefault,
        int loanPaymentTickMinutes,

        double corpFoundingCost,
        int corpDefaultTotalShares,
        double corpBaseDrift,
        double corpBaseVolatility,
        double corpProfitPriceSensitivity,
        int corpPriceTickMinutes,

        double wealthTaxPercent,
        int wealthTaxTickHours,
        double transactionTaxPercent,
        double corporateTaxPercent,
        int corporateTaxTickHours,
        boolean expenseWriteoffEnabled,
        double trustShieldPercent,
        double trustSetupCost
) {

    /** Highest-scoring tier the given score qualifies for, or the lowest tier if none match (score
     * below every configured minimum). Assumes at least one tier is configured. */
    public LoanTier tierFor(int score) {
        LoanTier best = loanTiers.get(loanTiers.size() - 1);
        for (LoanTier tier : loanTiers) {
            if (score >= tier.minScore()) {
                return tier;
            }
        }
        return best;
    }
}
