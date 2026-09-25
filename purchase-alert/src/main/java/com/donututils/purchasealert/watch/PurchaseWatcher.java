package com.donututils.purchasealert.watch;

import com.donututils.purchasealert.config.AlertConfig;
import com.donututils.purchasealert.discord.DiscordWebhook;
import com.donututils.purchasealert.http.BackendOrdersClient;
import com.donututils.purchasealert.http.OrderRecord;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Polls StoreBridge's backend (read-only, see {@link BackendOrdersClient}) for orders and posts
 * a Discord alert the moment one transitions to "delivered". Runs off the main thread since it's
 * pure HTTP with no Bukkit/world state involved.
 */
public final class PurchaseWatcher implements Runnable {

    private final Plugin plugin;
    private final BackendOrdersClient client;
    private final DiscordWebhook webhook;
    private final AlertConfig config;

    private final Set<String> announcedOrderIds = new HashSet<>();
    private boolean primed = false;

    public PurchaseWatcher(Plugin plugin, BackendOrdersClient client, DiscordWebhook webhook, AlertConfig config) {
        this.plugin = plugin;
        this.client = client;
        this.webhook = webhook;
        this.config = config;
    }

    @Override
    public void run() {
        List<OrderRecord> orders;
        try {
            orders = client.fetchRecentOrders(config.orderLimit());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
        } catch (IOException | RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to poll StoreBridge's backend for orders", ex);
            return;
        }

        if (!primed) {
            // Don't replay pre-existing order history as "new" the moment the plugin starts.
            for (OrderRecord order : orders) {
                if (isDelivered(order)) {
                    announcedOrderIds.add(order.id());
                }
            }
            primed = true;
            return;
        }

        for (OrderRecord order : orders) {
            if (!isDelivered(order) || announcedOrderIds.contains(order.id())) {
                continue;
            }
            announcedOrderIds.add(order.id());
            announce(order);
        }

        // Bound memory to roughly the size of the "recent orders" window instead of growing forever.
        Set<String> currentIds = new HashSet<>();
        for (OrderRecord order : orders) {
            currentIds.add(order.id());
        }
        announcedOrderIds.retainAll(currentIds);
    }

    private static boolean isDelivered(OrderRecord order) {
        return order.status() != null && order.status().equalsIgnoreCase("delivered");
    }

    private void announce(OrderRecord order) {
        String username = order.username() != null ? order.username() : "an unknown player";
        String product = order.product() != null ? order.product() : "an unknown item";
        webhook.sendAlert(
                "New store purchase",
                username + " just purchased **" + product + "**!",
                0x57F287,
                Map.of("Player", username, "Item", product)
        );
    }
}
