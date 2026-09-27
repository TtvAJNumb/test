package com.donututils.aichat.client;

import com.donututils.aichat.json.MiniJson;
import com.donututils.aichat.memory.ConversationMemory;
import com.donututils.aichat.tool.ServerContextService;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Client for a local (or self-hosted) Ollama server - completely free, no API key, runs on your own
 * hardware. Plain chat only, no tool-calling support yet.
 */
public final class OllamaApiClient implements ChatClient {

    private final Plugin plugin;
    private final HttpClient client;
    private final String baseUrl;

    public OllamaApiClient(Plugin plugin, String baseUrl) {
        this.plugin = plugin;
        this.baseUrl = baseUrl == null || baseUrl.isBlank() ? "http://localhost:11434" : stripTrailingSlash(baseUrl);
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public CompletableFuture<Reply> ask(String apiKey, String model, int maxTokens, String systemPrompt,
                                         List<ConversationMemory.Message> history, String userMessage,
                                         ServerContextService context, CommandSender sender) {
        List<Object> messages = new ArrayList<>();
        messages.add(mapOf("role", "system", "content", systemPrompt));
        for (ConversationMemory.Message message : history) {
            messages.add(mapOf("role", message.role(), "content", message.content()));
        }
        messages.add(mapOf("role", "user", "content", userMessage));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", false);
        body.put("options", mapOf("num_predict", (double) maxTokens));

        String json = MiniJson.write(body);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/chat"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (apiKey != null && !apiKey.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }

        return client.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                .handle(this::handleResponse);
    }

    @SuppressWarnings("unchecked")
    private Reply handleResponse(HttpResponse<String> response, Throwable error) {
        if (error != null) {
            plugin.getLogger().log(Level.WARNING, "Failed to reach Ollama at " + baseUrl, error);
            return new Reply(false, "Couldn't reach Ollama at " + baseUrl + " - is it running?");
        }
        if (response.statusCode() >= 300) {
            plugin.getLogger().warning("Ollama returned HTTP " + response.statusCode() + ": " + response.body());
            return new Reply(false, "Ollama returned an error (HTTP " + response.statusCode() + ").");
        }
        try {
            Map<String, Object> parsed = (Map<String, Object>) MiniJson.parse(response.body());
            Map<String, Object> message = (Map<String, Object>) parsed.get("message");
            String content = message == null ? null : (String) message.get("content");
            if (content == null || content.isBlank()) {
                return new Reply(false, "Ollama returned an empty response.");
            }
            return new Reply(true, content);
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, "Couldn't parse Ollama's response", ex);
            return new Reply(false, "Ollama returned an unexpected response.");
        }
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }
}
