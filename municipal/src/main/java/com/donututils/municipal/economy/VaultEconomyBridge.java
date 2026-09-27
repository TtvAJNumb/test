package com.donututils.municipal.economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

/** Talks to whatever economy plugin is registered with Vault, purely by reflection - same pattern
 * used by every other plugin in this repo except Ledger (which IS the economy). Works whether the
 * registered provider is Ledger's native economy, UDS, or anything else Vault-compatible. */
public final class VaultEconomyBridge {

    private final Object economy;
    private final Method getBalance;
    private final Method has;
    private final Method withdrawPlayer;
    private final Method depositPlayer;
    private final Method transactionSuccess;
    private final Field errorMessageField;

    private VaultEconomyBridge(Object economy, Method getBalance, Method has, Method withdrawPlayer,
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
    public static VaultEconomyBridge create() throws ReflectiveOperationException {
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

        return new VaultEconomyBridge(economy, getBalance, has, withdrawPlayer, depositPlayer,
                transactionSuccess, errorMessageField);
    }

    public double getBalance(UUID playerId) {
        return (double) call(getBalance, economy, offlinePlayer(playerId));
    }

    public boolean has(UUID playerId, double amount) {
        return (boolean) call(has, economy, offlinePlayer(playerId), amount);
    }

    public record EconomyResult(boolean success, String errorMessage) {
    }

    public EconomyResult withdraw(UUID playerId, double amount) {
        Object response = call(withdrawPlayer, economy, offlinePlayer(playerId), amount);
        boolean success = (boolean) call(transactionSuccess, response);
        return new EconomyResult(success, success ? null : readErrorMessage(response));
    }

    public EconomyResult deposit(UUID playerId, double amount) {
        Object response = call(depositPlayer, economy, offlinePlayer(playerId), amount);
        boolean success = (boolean) call(transactionSuccess, response);
        return new EconomyResult(success, success ? null : readErrorMessage(response));
    }

    private String readErrorMessage(Object response) {
        try {
            return (String) errorMessageField.get(response);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("errorMessage field is not accessible: " + ex, ex);
        }
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
