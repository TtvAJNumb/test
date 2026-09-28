package com.donututils.realworld.dynamicshop.currency;

import java.util.UUID;

/**
 * A single currency the shop can price items in. Each concrete implementation bridges to a real
 * economy plugin (Vault for money, PlayerPoints for shards) purely by reflection, the same
 * dependency-free pattern used throughout this repo, so this module never links against those
 * plugins' jars at compile time.
 */
public interface CurrencyProvider {

    /** Stable lowercase id used in config/commands/GUI filters, e.g. "money", "shards". */
    String id();

    /** Human-readable name shown in the GUI and chat, e.g. "Money", "Shards". */
    String displayName();

    double balance(UUID playerId);

    boolean has(UUID playerId, double amount);

    /** Returns true on success. */
    boolean withdraw(UUID playerId, double amount);

    void deposit(UUID playerId, double amount);

    String format(double amount);
}
