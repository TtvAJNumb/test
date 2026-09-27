package com.donututils.dynamicshop.gui;

@FunctionalInterface
public interface PriceFormatter {
    String format(String currencyId, double amount);
}
