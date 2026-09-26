package com.donututils.marketwatch.http;

import com.donututils.marketwatch.config.MarketWatchConfig;
import com.donututils.marketwatch.model.EconomySnapshot;
import com.donututils.marketwatch.service.MarketStatsService;
import com.donututils.marketwatch.storage.EconomySnapshotStore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * A tiny read-only JSON feed for a website chart. Deliberately built on the JDK's own
 * {@code com.sun.net.httpserver} (no new dependency) and, by default, bound to localhost only -
 * anyone exposing this further is expected to put their own reverse proxy/firewall in front of it.
 */
public final class MarketHttpServer {

    private static final int MAX_HISTORY_POINTS = 2000;

    private final Plugin plugin;
    private final Supplier<MarketWatchConfig> configSupplier;
    private final EconomySnapshotStore economyStore;
    private final MarketStatsService statsService;
    private HttpServer server;

    public MarketHttpServer(Plugin plugin, Supplier<MarketWatchConfig> configSupplier,
                             EconomySnapshotStore economyStore, MarketStatsService statsService) {
        this.plugin = plugin;
        this.configSupplier = configSupplier;
        this.economyStore = economyStore;
        this.statsService = statsService;
    }

    public void start() {
        MarketWatchConfig config = configSupplier.get();
        if (!config.httpEnabled()) {
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress(config.httpBindAddress(), config.httpPort()), 0);
            server.createContext("/api/economy/history", authenticated(this::handleEconomyHistory));
            server.createContext("/api/market/items", authenticated(this::handleTopItems));
            server.createContext("/api/market/item/", authenticated(this::handleSingleItem));
            server.setExecutor(Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "MarketWatch-HTTP");
                thread.setDaemon(true);
                return thread;
            }));
            server.start();
            plugin.getLogger().info("MarketWatch HTTP feed listening on " + config.httpBindAddress() + ":" + config.httpPort());
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to start MarketWatch HTTP feed", ex);
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
                plugin.getLogger().log(Level.WARNING, "MarketWatch HTTP handler failed", ex);
                sendJson(exchange, 500, "{\"error\":\"internal error\"}");
            }
        };
    }

    private void handleEconomyHistory(HttpExchange exchange) throws IOException {
        List<EconomySnapshot> snapshots = economyStore.readAll();
        int from = Math.max(0, snapshots.size() - MAX_HISTORY_POINTS);
        StringBuilder json = new StringBuilder("[");
        for (int i = from; i < snapshots.size(); i++) {
            EconomySnapshot snapshot = snapshots.get(i);
            if (i > from) {
                json.append(',');
            }
            json.append("{\"timestamp\":").append(snapshot.timestampMillis())
                    .append(",\"totalMoney\":").append(snapshot.totalMoney())
                    .append(",\"playerCount\":").append(snapshot.playerCount())
                    .append('}');
        }
        json.append(']');
        sendJson(exchange, 200, json.toString());
    }

    private void handleTopItems(HttpExchange exchange) throws IOException {
        long windowMillis = parseWindowMillis(exchange, 7);
        List<MarketStatsService.ItemStats> items = statsService.getTopTradedItems(windowMillis, 50);
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            appendItemStats(json, items.get(i));
        }
        json.append(']');
        sendJson(exchange, 200, json.toString());
    }

    private void handleSingleItem(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String material = path.substring(path.lastIndexOf('/') + 1).toUpperCase(Locale.ROOT);
        long windowMillis = parseWindowMillis(exchange, 7);
        MarketStatsService.ItemStats stats = statsService.getItemStats(material, windowMillis);
        StringBuilder json = new StringBuilder();
        appendItemStats(json, stats);
        sendJson(exchange, 200, json.toString());
    }

    private void appendItemStats(StringBuilder json, MarketStatsService.ItemStats stats) {
        json.append("{\"material\":\"").append(stats.material()).append('"')
                .append(",\"avgPrice\":").append(stats.avgPrice())
                .append(",\"minPrice\":").append(stats.minPrice())
                .append(",\"maxPrice\":").append(stats.maxPrice())
                .append(",\"volume\":").append(stats.volume())
                .append('}');
    }

    private long parseWindowMillis(HttpExchange exchange, int defaultDays) {
        String query = exchange.getRequestURI().getQuery();
        int days = defaultDays;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && kv[0].equals("days")) {
                    try {
                        days = Math.max(1, Integer.parseInt(kv[1]));
                    } catch (NumberFormatException ignored) {
                        // keep default
                    }
                }
            }
        }
        return days * 24L * 60L * 60L * 1000L;
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
