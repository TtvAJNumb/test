package com.donututils.marketwatch.reflect;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Talks to UltimateDonutSmp's real, public LeaderboardManager and AuctionHouseManager by
 * reflection - every method resolved here is verified against the plugin's actual source, not
 * decompiled guesswork. Nothing here reaches into a private field.
 */
public final class UdsBridge {

    private final Plugin uds;

    private final Method getLeaderboardManager;
    private final Method getEntries;
    private final Method getTotalEntries;
    private final Method entryPlayerData;
    private final Method playerDataGetMoney;
    private final Object moneyType;

    private final Method getAuctionHouseManager;
    private final Method getActiveListings;
    private final Method getListing;
    private final Object newestSort;

    private UdsBridge(Plugin uds) throws ReflectiveOperationException {
        this.uds = uds;
        Class<?> pluginClass = uds.getClass();

        this.getLeaderboardManager = pluginClass.getMethod("getLeaderboardManager");
        Class<?> leaderboardManagerClass = getLeaderboardManager.getReturnType();
        Class<?> leaderboardTypeClass = Class.forName(leaderboardManagerClass.getName() + "$LeaderboardType");
        this.moneyType = leaderboardTypeClass.getMethod("valueOf", String.class).invoke(null, "MONEY");
        this.getEntries = leaderboardManagerClass.getMethod("getEntries", leaderboardTypeClass, int.class, int.class);
        this.getTotalEntries = leaderboardManagerClass.getMethod("getTotalEntries", leaderboardTypeClass);
        Class<?> entryClass = Class.forName(leaderboardManagerClass.getName() + "$LeaderboardEntry");
        this.entryPlayerData = entryClass.getMethod("playerData");
        Class<?> playerDataClass = Class.forName("com.bx.ultimateDonutSmp.models.PlayerData");
        this.playerDataGetMoney = playerDataClass.getMethod("getMoney");

        this.getAuctionHouseManager = pluginClass.getMethod("getAuctionHouseManager");
        Class<?> auctionManagerClass = getAuctionHouseManager.getReturnType();
        Class<?> auctionSortClass = Class.forName(auctionManagerClass.getName() + "$AuctionSort");
        this.newestSort = auctionSortClass.getMethod("valueOf", String.class).invoke(null, "NEWEST");
        this.getActiveListings = auctionManagerClass.getMethod("getActiveListings", auctionSortClass);
        this.getListing = auctionManagerClass.getMethod("getListing", long.class);
    }

    public static UdsBridge create(Plugin uds) throws ReflectiveOperationException {
        return new UdsBridge(uds);
    }

    /** Total money currently held by every known player (online and offline). */
    @SuppressWarnings("unchecked")
    public double getTotalCirculatingMoney() {
        Object leaderboardManager = call(getLeaderboardManager, uds);
        int total = (int) call(getTotalEntries, leaderboardManager, moneyType);
        List<Object> entries = (List<Object>) call(getEntries, leaderboardManager, moneyType, 0, total);
        double sum = 0;
        for (Object entry : entries) {
            Object playerData = call(entryPlayerData, entry);
            sum += (double) call(playerDataGetMoney, playerData);
        }
        return sum;
    }

    public int getKnownPlayerCount() {
        Object leaderboardManager = call(getLeaderboardManager, uds);
        return (int) call(getTotalEntries, leaderboardManager, moneyType);
    }

    public record ActiveListing(long id, String category, long soldAt, String status, String material, double price) {
    }

    @SuppressWarnings("unchecked")
    public List<ActiveListing> getActiveListings() {
        Object auctionManager = call(getAuctionHouseManager, uds);
        List<Object> raw = (List<Object>) call(getActiveListings, auctionManager, newestSort);
        List<ActiveListing> listings = new ArrayList<>();
        for (Object listing : raw) {
            listings.add(toActiveListing(listing));
        }
        return listings;
    }

    /** Looks a listing up by id regardless of its current status (active, sold, expired, cancelled). */
    public ActiveListing getListingById(long listingId) {
        Object auctionManager = call(getAuctionHouseManager, uds);
        Object listing = call(getListing, auctionManager, listingId);
        return listing == null ? null : toActiveListing(listing);
    }

    private ActiveListing toActiveListing(Object listing) {
        long id = (long) invoke(listing, "id");
        String category = (String) invoke(listing, "category");
        long soldAt = (long) invoke(listing, "soldAt");
        Object statusObj = invoke(listing, "status");
        String status = statusObj == null ? "" : (String) invoke(statusObj, "name");
        double price = (double) invoke(listing, "price");
        Object item = invoke(listing, "item");
        String material = item instanceof ItemStack stack && stack.getType() != null ? stack.getType().name() : "UNKNOWN";
        return new ActiveListing(id, category, soldAt, status, material, price);
    }

    private static Object invoke(Object target, String noArgMethodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(noArgMethodName);
            return call(method, target);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Missing expected method " + noArgMethodName + " on " + target.getClass(), ex);
        }
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
