package com.donututils.realworld.dynamicshop.service;

import com.donututils.realworld.dynamicshop.config.DynamicShopConfig;
import com.donututils.realworld.dynamicshop.currency.CurrencyProvider;
import com.donututils.realworld.dynamicshop.currency.CurrencyRegistry;
import com.donututils.realworld.dynamicshop.model.Loan;
import com.donututils.realworld.dynamicshop.storage.LoanStore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Advanced loans with interest, gated entirely behind {@code features.loans} in config. v1 supports
 * one active loan per player at a time. */
public final class LoanService {

    public record LoanResult(boolean success, String message) {
    }

    private final LoanStore store;
    private final Map<UUID, Loan> loans;
    private final CurrencyRegistry currencyRegistry;
    private final Supplier<DynamicShopConfig> configSupplier;

    public LoanService(LoanStore store, CurrencyRegistry currencyRegistry, Supplier<DynamicShopConfig> configSupplier) {
        this.store = store;
        this.loans = new ConcurrentHashMap<>(store.load());
        this.currencyRegistry = currencyRegistry;
        this.configSupplier = configSupplier;
    }

    public LoanResult borrow(UUID playerId, double amount, String currencyId) {
        DynamicShopConfig config = configSupplier.get();
        if (!config.loansFeatureEnabled()) {
            return new LoanResult(false, "Loans are disabled on this server.");
        }
        if (loans.containsKey(playerId)) {
            return new LoanResult(false, "You already have an outstanding loan. Pay it off first with /shop repay <amount>.");
        }
        if (amount <= 0 || amount > config.maxLoanAmount()) {
            return new LoanResult(false, "Loan amount must be between 0 and " + config.maxLoanAmount() + ".");
        }
        CurrencyProvider currency = currencyRegistry.get(currencyId);
        if (currency == null) {
            return new LoanResult(false, "Unknown or disabled currency: " + currencyId);
        }
        Loan loan = new Loan(currencyId, amount, config.interestRatePercentPerDay(), System.currentTimeMillis());
        loans.put(playerId, loan);
        currency.deposit(playerId, amount);
        store.save(loans);
        return new LoanResult(true, "Borrowed " + currency.format(amount) + " at " + config.interestRatePercentPerDay() + "%/day interest.");
    }

    public LoanResult repay(UUID playerId, double amount) {
        Loan loan = loans.get(playerId);
        if (loan == null) {
            return new LoanResult(false, "You don't have an outstanding loan.");
        }
        loan.accrue(System.currentTimeMillis());
        CurrencyProvider currency = currencyRegistry.get(loan.currency());
        if (currency == null) {
            return new LoanResult(false, "That loan's currency isn't available right now.");
        }
        double payment = Math.min(amount, loan.principal());
        if (payment <= 0) {
            return new LoanResult(false, "Enter a positive amount.");
        }
        if (!currency.has(playerId, payment)) {
            return new LoanResult(false, "You don't have enough " + currency.displayName() + " to pay that much.");
        }
        if (!currency.withdraw(playerId, payment)) {
            return new LoanResult(false, "The payment failed - try again.");
        }
        loan.setPrincipal(loan.principal() - payment);
        String message;
        if (loan.isPaidOff()) {
            loans.remove(playerId);
            message = "Paid off your loan in full!";
        } else {
            message = "Paid " + currency.format(payment) + " - " + currency.format(loan.principal()) + " remaining.";
        }
        store.save(loans);
        return new LoanResult(true, message);
    }

    public Loan get(UUID playerId) {
        Loan loan = loans.get(playerId);
        if (loan != null) {
            loan.accrue(System.currentTimeMillis());
        }
        return loan;
    }

    public double totalOutstandingDebt() {
        double total = 0;
        long now = System.currentTimeMillis();
        for (Loan loan : loans.values()) {
            loan.accrue(now);
            total += loan.principal();
        }
        return total;
    }

    public void saveAll() {
        store.save(loans);
    }
}
