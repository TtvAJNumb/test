package com.donututils.realworld.careers.economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

/** Talks to whatever economy plugin is registered with Vault, purely by reflection - same pattern
 * used by DynamicShop, Municipal, and Motors (Careers is a consumer of the economy, not the economy
 * itself - that's Ledger's job). Only needs deposit, since Careers only ever pays players. */
public final class VaultEconomyBridge {

    private final Object economy;
    private final Method depositPlayer;

    private VaultEconomyBridge(Object economy, Method depositPlayer) {
        this.economy = economy;
        this.depositPlayer = depositPlayer;
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

        Method depositPlayer = economyClass.getMethod("depositPlayer", OfflinePlayer.class, double.class);
        return new VaultEconomyBridge(economy, depositPlayer);
    }

    public void deposit(UUID playerId, double amount) {
        call(depositPlayer, economy, offlinePlayer(playerId), amount);
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
