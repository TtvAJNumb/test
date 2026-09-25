package com.donututils.ecowatch.discord;

import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Minimal Discord webhook client. Deliberately dependency-free (no JSON library, no HTTP
 * library beyond the JDK's own {@link HttpClient}) so this plugin drops in as a single jar.
 */
public final class DiscordWebhook {

    private final Plugin plugin;
    private final HttpClient client;
    private String webhookUrl;
    private String username;

    public DiscordWebhook(Plugin plugin) {
        this.plugin = plugin;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void configure(String webhookUrl, String username) {
        this.webhookUrl = webhookUrl;
        this.username = username;
    }

    public boolean isConfigured() {
        return webhookUrl != null && !webhookUrl.isBlank();
    }

    /** Sends one embed asynchronously. title/description/color drive the embed; fields are label -> value. */
    public void sendAlert(String title, String description, int color, Map<String, String> fields) {
        if (!isConfigured()) {
            plugin.getLogger().warning("Discord webhook is not configured (discord.webhook-url in config.yml); dropping alert: " + title);
            return;
        }
        String body = buildPayload(title, description, color, fields);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .whenComplete((response, error) -> {
                    if (error != null) {
                        plugin.getLogger().log(Level.WARNING, "Failed to deliver Discord alert", error);
                    } else if (response.statusCode() >= 300) {
                        plugin.getLogger().warning("Discord webhook returned HTTP " + response.statusCode() + " for alert: " + title);
                    }
                });
    }

    private String buildPayload(String title, String description, int color, Map<String, String> fields) {
        StringBuilder fieldsJson = new StringBuilder();
        if (fields != null) {
            List<Map.Entry<String, String>> entries = List.copyOf(fields.entrySet());
            for (int i = 0; i < entries.size(); i++) {
                Map.Entry<String, String> entry = entries.get(i);
                if (i > 0) {
                    fieldsJson.append(',');
                }
                fieldsJson.append("{\"name\":").append(json(entry.getKey()))
                        .append(",\"value\":").append(json(entry.getValue()))
                        .append(",\"inline\":true}");
            }
        }

        return "{"
                + "\"username\":" + json(username == null ? "Economy Watchdog" : username) + ","
                + "\"embeds\":[{"
                + "\"title\":" + json(title) + ","
                + "\"description\":" + json(description) + ","
                + "\"color\":" + color + ","
                + "\"timestamp\":" + json(Instant.now().toString()) + ","
                + "\"fields\":[" + fieldsJson + "]"
                + "}]"
                + "}";
    }

    private static String json(String value) {
        if (value == null) {
            return "\"\"";
        }
        StringBuilder out = new StringBuilder(value.length() + 2);
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
        return out.toString();
    }
}
