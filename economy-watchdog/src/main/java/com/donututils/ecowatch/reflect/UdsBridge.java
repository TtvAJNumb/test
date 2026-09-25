package com.donututils.ecowatch.reflect;

import com.donututils.ecowatch.model.AuctionSaleSnapshot;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Talks to UltimateDonutSmp's internal EconomyManager and AuctionHouseManager purely by
 * reflection, the same approach CrateBindAddon uses for the crate API. UDS ships no events or
 * persisted log for balance changes or auction sales, so this bridge exposes two things a
 * watchdog can actually poll:
 * <ul>
 *   <li>{@link #getBalance(UUID)} - current balance, straight off the public API.</li>
 *   <li>{@link #getSoldListingsSnapshot()} - a read of AuctionHouseManager's private in-memory
 *   listing cache (the same cache {@code getActiveListings} filters down from), because there is
 *   no public "recently sold" query. This is the one place this plugin reaches past a public
 *   method into a private field, and it is the most likely thing to break on a UDS update - see
 *   the null/empty handling in the caller.</li>
 * </ul>
 */
public final class UdsBridge {

    private final Plugin uds;
    private final Method getEconomyManager;
    private final Method getBalance;
    private final Method getAuctionHouseManager;
    private final Field listingCacheField;

    private UdsBridge(Plugin uds) throws ReflectiveOperationException {
        this.uds = uds;
        Class<?> pluginClass = uds.getClass();

        this.getEconomyManager = pluginClass.getMethod("getEconomyManager");
        Class<?> economyClass = this.getEconomyManager.getReturnType();
        this.getBalance = economyClass.getMethod("getBalance", UUID.class);

        this.getAuctionHouseManager = pluginClass.getMethod("getAuctionHouseManager");
        Class<?> auctionManagerClass = this.getAuctionHouseManager.getReturnType();
        Field field = auctionManagerClass.getDeclaredField("listingCache");
        field.setAccessible(true);
        this.listingCacheField = field;
    }

    public static UdsBridge create(Plugin uds) throws ReflectiveOperationException {
        return new UdsBridge(uds);
    }

    public double getBalance(UUID uuid) {
        Object manager = call(getEconomyManager, uds);
        return (double) call(getBalance, manager, uuid);
    }

    /**
     * Best-effort read of every SOLD listing currently sitting in UDS's in-memory auction
     * cache. Returns an empty list (rather than throwing) if the cache is empty or not yet
     * populated - callers should treat that the same as "nothing sold since last poll".
     */
    @SuppressWarnings("unchecked")
    public List<AuctionSaleSnapshot> getSoldListingsSnapshot() throws ReflectiveOperationException {
        Object manager = call(getAuctionHouseManager, uds);
        Object rawRef = listingCacheField.get(manager);
        if (!(rawRef instanceof AtomicReference<?> ref)) {
            return List.of();
        }
        Object listObj = ref.get();
        List<AuctionSaleSnapshot> sales = new ArrayList<>();
        if (!(listObj instanceof List<?> listings)) {
            return sales;
        }
        for (Object listing : listings) {
            Object statusObj = invoke(listing, "status");
            String status = statusObj == null ? "" : (String) invoke(statusObj, "name");
            if (!"SOLD".equals(status)) {
                continue;
            }
            long id = (long) invoke(listing, "id");
            UUID seller = (UUID) invoke(listing, "sellerUuid");
            String sellerName = (String) invoke(listing, "sellerName");
            UUID buyer = (UUID) invoke(listing, "buyerUuid");
            double price = (double) invoke(listing, "price");
            long soldAt = (long) invoke(listing, "soldAt");
            String category = (String) invoke(listing, "category");
            sales.add(new AuctionSaleSnapshot(id, seller, sellerName, buyer, price, soldAt, category));
        }
        return sales;
    }

    private static Object invoke(Object target, String noArgMethodName) throws ReflectiveOperationException {
        if (target == null) {
            return null;
        }
        Method method = target.getClass().getMethod(noArgMethodName);
        return call(method, target);
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
