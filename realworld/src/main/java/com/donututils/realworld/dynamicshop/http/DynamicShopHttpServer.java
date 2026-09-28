package com.donututils.realworld.dynamicshop.http;

import com.donututils.realworld.dynamicshop.config.DynamicShopConfig;
import com.donututils.realworld.dynamicshop.engine.ShopItemRegistry;
import com.donututils.realworld.dynamicshop.model.ShopItem;
import com.donututils.realworld.dynamicshop.service.EconomyStatsService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * A tiny read-only JSON feed of shop prices and economy stats, gated behind {@code features.web-server}.
 * Same JDK-only {@code com.sun.net.httpserver} approach as MarketWatch's HTTP feed - no new
 * dependency, bound to localhost by default.
 */
public final class DynamicShopHttpServer {

    private final Plugin plugin;
    private final Supplier<DynamicShopConfig> configSupplier;
    private final ShopItemRegistry itemRegistry;
    private final EconomyStatsService statsService;
    private HttpServer server;

    public DynamicShopHttpServer(Plugin plugin, Supplier<DynamicShopConfig> configSupplier,
                                  ShopItemRegistry itemRegistry, EconomyStatsService statsService) {
        this.plugin = plugin;
        this.configSupplier = configSupplier;
        this.itemRegistry = itemRegistry;
        this.statsService = statsService;
    }

    public void start() {
        DynamicShopConfig config = configSupplier.get();
        if (!config.webServerFeatureEnabled()) {
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress(config.httpBindAddress(), config.httpPort()), 0);
            server.createContext("/api/items", authenticated(this::handleItems));
            server.createContext("/api/economy", authenticated(this::handleEconomy));
            server.setExecutor(Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "DynamicShop-HTTP");
                thread.setDaemon(true);
                return thread;
            }));
            server.start();
            plugin.getLogger().info("DynamicShop HTTP feed listening on " + config.httpBindAddress() + ":" + config.httpPort());
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to start DynamicShop HTTP feed", ex);
            server = null;
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private HttpHandler authenticated(HttpHandler delegate) {
        return exchange -> {
            String apiKey = configSupplier.get().httpApiKey();
            if (apiKey != null && !apiKey.isBlank()) {
                String provided = exchange.getRequestHeaders().getFirst("X-Api-Key");
                if (!apiKey.equals(provided)) {
                    sendJson(exchange, 401, "{\"error\":\"unauthorized\"}");
                    return;
                }
            }
            try {
                delegate.handle(exchange);
            } catch (RuntimeException ex) {
                plugin.getLogger().log(Level.WARNING, "DynamicShop HTTP handler failed", ex);
                sendJson(exchange, 500, "{\"error\":\"internal error\"}");
            }
        };
    }

    private void handleItems(HttpExchange exchange) throws IOException {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (ShopItem item : itemRegistry.all()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append("{\"material\":\"").append(item.material()).append('"')
                    .append(",\"displayName\":\"").append(escape(item.displayName())).append('"')
                    .append(",\"category\":\"").append(escape(item.category())).append('"')
                    .append(",\"currency\":\"").append(item.currency()).append('"')
                    .append(",\"price\":").append(item.currentPrice())
                    .append(",\"basePrice\":").append(item.basePrice())
                    .append(",\"buyEnabled\":").append(item.buyEnabled())
                    .append(",\"sellEnabled\":").append(item.sellEnabled())
                    .append('}');
        }
        json.append(']');
        sendJson(exchange, 200, json.toString());
    }

    private void handleEconomy(HttpExchange exchange) throws IOException {
        EconomyStatsService.EconomyStats stats = statsService.compute();
        String json = "{\"gdp\":" + stats.gdp()
                + ",\"totalDebt\":" + stats.totalDebt()
                + ",\"inflationPercent\":" + stats.inflationPercent()
                + ",\"transactionCount\":" + stats.transactionCount()
                + "}";
        sendJson(exchange, 200, json);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
