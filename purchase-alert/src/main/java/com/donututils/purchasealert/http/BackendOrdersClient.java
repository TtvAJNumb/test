package com.donututils.purchasealert.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Talks to the same backend StoreBridge polls, but only ever calls its read-only order-listing
 * endpoint - the exact one StoreBridge's own "/storebridge orders" admin command reads from
 * (confirmed by decompiling StoreBridge's BackendClient class). This never calls the endpoints
 * that actually claim or mark an order delivered, so it cannot affect StoreBridge's own delivery
 * flow in any way - it's a second, independent reader of the same data, nothing more.
 * <p>
 * Uses {@code com.google.gson} for JSON parsing rather than a hand-rolled parser: Gson ships
 * bundled with every Spigot/Paper server jar (Minecraft itself depends on it), so it's available
 * without adding any dependency to this plugin's jar.
 */
public final class BackendOrdersClient {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final String backendUrl;
    private final String pluginKey;

    public BackendOrdersClient(String backendUrl, String pluginKey) {
        String trimmed = backendUrl == null ? "" : backendUrl.trim();
        this.backendUrl = trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        this.pluginKey = pluginKey == null ? "" : pluginKey.trim();
    }

    public List<OrderRecord> fetchRecentOrders(int limit) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(backendUrl + "/api/plugin/orders?limit=" + limit))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + pluginKey)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401) {
            throw new IOException("Backend rejected the plugin key (HTTP 401) - check plugin_key in config.yml");
        }
        if (response.statusCode() != 200) {
            throw new IOException("Backend returned HTTP " + response.statusCode());
        }

        JsonElement root = JsonParser.parseString(response.body());
        if (!root.isJsonObject() || !root.getAsJsonObject().has("orders")) {
            return List.of();
        }
        JsonArray orders = root.getAsJsonObject().getAsJsonArray("orders");

        List<OrderRecord> out = new ArrayList<>();
        for (JsonElement element : orders) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject o = element.getAsJsonObject();
            String id = stringField(o, "id");
            if (id == null) {
                continue;
            }
            out.add(new OrderRecord(id, stringField(o, "username"), stringField(o, "product"),
                    stringField(o, "status"), stringField(o, "event")));
        }
        return out;
    }

    private static String stringField(JsonObject o, String key) {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            return null;
        }
        JsonElement value = o.get(key);
        return value.isJsonPrimitive() ? value.getAsString() : null;
    }
}
