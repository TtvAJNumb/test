package com.donututils.realworld.dynamicshop.currency;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;

/**
 * The "shards" currency, bridged to the PlayerPoints plugin purely by reflection (its
 * {@code getAPI()} method on the plugin instance returns a {@code PlayerPointsAPI} with
 * look/give/take methods). No compile-time dependency on PlayerPoints' jar.
 */
public final class PlayerPointsCurrencyBridge implements CurrencyProvider {

    private final Object api;
    private final Method look;
    private final Method give;
    private final Method take;
    private final String displayName;

    private PlayerPointsCurrencyBridge(Object api, Method look, Method give, Method take, String displayName) {
        this.api = api;
        this.look = look;
        this.give = give;
        this.take = take;
        this.displayName = displayName;
    }

    /** Returns null if PlayerPoints isn't installed/enabled. */
    public static PlayerPointsCurrencyBridge create(String displayName) throws ReflectiveOperationException {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PlayerPoints");
        if (plugin == null || !plugin.isEnabled()) {
            return null;
        }
        Method getApi = plugin.getClass().getMethod("getAPI");
        Object api = getApi.invoke(plugin);
        if (api == null) {
            return null;
        }
        Class<?> apiClass = Class.forName("org.black_ixx.playerpoints.PlayerPointsAPI");
        Method look = apiClass.getMethod("look", UUID.class);
        Method give = apiClass.getMethod("give", UUID.class, int.class);
        Method take = apiClass.getMethod("take", UUID.class, int.class);
        return new PlayerPointsCurrencyBridge(api, look, give, take, displayName);
    }

    @Override
    public String id() {
        return "shards";
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public double balance(UUID playerId) {
        return ((Number) call(look, playerId)).doubleValue();
    }

    @Override
    public boolean has(UUID playerId, double amount) {
        return balance(playerId) >= amount;
    }

    @Override
    public boolean withdraw(UUID playerId, double amount) {
        if (!has(playerId, amount)) {
            return false;
        }
        return (boolean) call(take, playerId, (int) Math.round(amount));
    }

    @Override
    public void deposit(UUID playerId, double amount) {
        call(give, playerId, (int) Math.round(amount));
    }

    @Override
    public String format(double amount) {
        return String.format(Locale.US, "%,.0f %s", amount, displayName);
    }

    private Object call(Method method, Object... args) {
        try {
            return method.invoke(api, args);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new IllegalStateException(method.getName() + " failed: " + cause, cause);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(method.getName() + " is not accessible: " + ex, ex);
        }
    }
}
