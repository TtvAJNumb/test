package com.donututils.ecowatch.watch;

import com.donututils.ecowatch.config.EcoWatchConfig;
import com.donututils.ecowatch.discord.DiscordWebhook;
import com.donututils.ecowatch.model.AuctionSaleSnapshot;
import com.donututils.ecowatch.reflect.UdsBridge;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Polls UltimateDonutSmp's in-memory auction listing cache each cycle for newly SOLD listings
 * and flags unusually high or unusually low sale prices, plus the same buyer/seller pair
 * trading with each other suspiciously often (a common alt-account laundering pattern).
 */
public final class AuctionWatcher implements Runnable {

    private final Plugin plugin;
    private final UdsBridge bridge;
    private final DiscordWebhook webhook;
    private final EcoWatchConfig config;

    private final Set<Long> seenListingIds = new HashSet<>();
    private final Map<String, Deque<Long>> pairTradeTimestamps = new HashMap<>();
    private boolean primed = false;

    public AuctionWatcher(Plugin plugin, UdsBridge bridge, DiscordWebhook webhook, EcoWatchConfig config) {
        this.plugin = plugin;
        this.bridge = bridge;
        this.webhook = webhook;
        this.config = config;
    }

    @Override
    public void run() {
        if (!config.auctionEnabled()) {
            return;
        }

        List<AuctionSaleSnapshot> sales;
        try {
            sales = bridge.getSoldListingsSnapshot();
        } catch (ReflectiveOperationException | RuntimeException ex) {
            plugin.getLogger().warning("Failed to read auction house sales (UDS internals may have changed): " + ex.getMessage());
            return;
        }

        Set<Long> currentIds = new HashSet<>();
        for (AuctionSaleSnapshot sale : sales) {
            currentIds.add(sale.listingId());
        }

        if (!primed) {
            // First run: remember what's already sold so we don't replay pre-existing sales as "new".
            seenListingIds.addAll(currentIds);
            primed = true;
            return;
        }

        for (AuctionSaleSnapshot sale : sales) {
            if (seenListingIds.contains(sale.listingId())) {
                continue;
            }
            evaluate(sale);
        }

        // Bound memory to roughly the cache's current size instead of growing forever.
        seenListingIds.retainAll(currentIds);
        seenListingIds.addAll(currentIds);
    }

    private void evaluate(AuctionSaleSnapshot sale) {
        String sellerName = sale.sellerName() != null ? sale.sellerName() : "unknown";
        String buyerName = sale.buyerUuid() != null ? resolveName(sale.buyerUuid()) : "unknown";
        String category = sale.category() != null ? sale.category() : "ALL";

        if (sale.price() >= config.highPriceFlag()) {
            webhook.sendAlert(
                    "High-value auction sale",
                    sellerName + " sold an item for $" + format(sale.price()) + " to " + buyerName + ".",
                    0xEB459E,
                    Map.of("Seller", sellerName, "Buyer", buyerName, "Price", "$" + format(sale.price()), "Category", category)
            );
        } else if (sale.price() <= config.lowPriceFlag()) {
            webhook.sendAlert(
                    "Possible auction dump",
                    sellerName + " sold an item to " + buyerName + " for only $" + format(sale.price()) + ".",
                    0xED4245,
                    Map.of("Seller", sellerName, "Buyer", buyerName, "Price", "$" + format(sale.price()), "Category", category)
            );
        }

        if (config.repeatTradingEnabled() && sale.sellerUuid() != null && sale.buyerUuid() != null) {
            trackRepeatTrading(sale, sellerName, buyerName);
        }
    }

    private void trackRepeatTrading(AuctionSaleSnapshot sale, String sellerName, String buyerName) {
        String key = sale.sellerUuid() + ":" + sale.buyerUuid();
        Deque<Long> timestamps = pairTradeTimestamps.computeIfAbsent(key, k -> new ArrayDeque<>());
        long windowMillis = config.repeatWindowMinutes() * 60_000L;
        long now = sale.soldAt() > 0 ? sale.soldAt() : System.currentTimeMillis();

        timestamps.addLast(now);
        while (!timestamps.isEmpty() && now - timestamps.peekFirst() > windowMillis) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= config.repeatMinTrades()) {
            webhook.sendAlert(
                    "Repeated auction trading between two players",
                    sellerName + " and " + buyerName + " have completed " + timestamps.size()
                            + " auction trades in the last " + config.repeatWindowMinutes() + " minutes.",
                    0xFEE75C,
                    Map.of("Seller", sellerName, "Buyer", buyerName, "Trades", String.valueOf(timestamps.size()))
            );
            timestamps.clear(); // don't re-alert on every subsequent trade in the same burst
        }
    }

    private String resolveName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() != null ? player.getName() : uuid.toString().substring(0, 8);
    }

    private static String format(double value) {
        return String.format(Locale.US, "%,.2f", value);
    }
}
