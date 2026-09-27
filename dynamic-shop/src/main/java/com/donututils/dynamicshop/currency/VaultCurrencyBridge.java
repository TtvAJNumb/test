package com.donututils.dynamicshop.currency;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;

/**
 * The "money" currency, talking to whatever economy plugin is registered with Vault, purely by
 * reflection - same approach as StockMarket's VaultEconomyBridge, kept as its own copy here since
 * this module has no compile dependency on that one.
 */
public final class VaultCurrencyBridge implements CurrencyProvider {

    private final Object economy;
    private final Method getBalance;
    private final Method has;
    private final Method withdrawPlayer;
    private final Method depositPlayer;
    private final Method transactionSuccess;
    private final Field errorMessageField;

    private VaultCurrencyBridge(Object economy, Method getBalance, Method has, Method withdrawPlayer,
                                 Method depositPlayer, Method transactionSuccess, Field errorMessageField) {
        this.economy = economy;
        this.getBalance = getBalance;
        this.has = has;
        this.withdrawPlayer = withdrawPlayer;
        this.depositPlayer = depositPlayer;
        this.transactionSuccess = transactionSuccess;
        this.errorMessageField = errorMessageField;
    }

    /** Returns null if Vault isn't installed or has no economy provider registered yet. */
    public static VaultCurrencyBridge create() throws ReflectiveOperationException {
        ServicesManager servicesManager = Bukkit.getServicesManager();
        Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");

        Method getRegistration = ServicesManager.class.getMethod("getRegistration", Class.class);
        Object registration = getRegistration.invoke(servicesManager, economyClass);
        if (registration == null) {
            return null;
        }
        Method getProvider = RegisteredServiceProvider.class.getMethod("getProvider");
        Object economy = getProvider.invoke(registration);
        if (economy == null) {
            return null;
        }

        Method getBalance = economyClass.getMethod("getBalance", OfflinePlayer.class);
        Method has = economyClass.getMethod("has", OfflinePlayer.class, double.class);
        Method withdrawPlayer = economyClass.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
        Method depositPlayer = economyClass.getMethod("depositPlayer", OfflinePlayer.class, double.class);

        Class<?> responseClass = Class.forName("net.milkbowl.vault.economy.EconomyResponse");
        Method transactionSuccess = responseClass.getMethod("transactionSuccess");
        Field errorMessageField = responseClass.getField("errorMessage");

        return new VaultCurrencyBridge(economy, getBalance, has, withdrawPlayer, depositPlayer,
                transactionSuccess, errorMessageField);
    }

    @Override
    public String id() {
        return "money";
    }

    @Override
    public String displayName() {
        return "Money";
    }

    @Override
    public double balance(UUID playerId) {
        return (double) call(getBalance, economy, offlinePlayer(playerId));
    }

    @Override
    public boolean has(UUID playerId, double amount) {
        return (boolean) call(has, economy, offlinePlayer(playerId), amount);
    }

    @Override
    public boolean withdraw(UUID playerId, double amount) {
        Object response = call(withdrawPlayer, economy, offlinePlayer(playerId), amount);
        return (boolean) call(transactionSuccess, response);
    }

    @Override
    public void deposit(UUID playerId, double amount) {
        call(depositPlayer, economy, offlinePlayer(playerId), amount);
    }

    @Override
    public String format(double amount) {
        return String.format(Locale.US, "$%,.2f", amount);
    }

    private static OfflinePlayer offlinePlayer(UUID playerId) {
        return Bukkit.getOfflinePlayer(playerId);
    }

    private static Object call(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new IllegalStateException(method.getName() + " failed: " + cause, cause);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(method.getName() + " is not accessible: " + ex, ex);
        }
    }
}
