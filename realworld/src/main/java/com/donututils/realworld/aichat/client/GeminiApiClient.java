package com.donututils.realworld.aichat.client;

import com.donututils.realworld.aichat.json.MiniJson;
import com.donututils.realworld.aichat.memory.ConversationMemory;
import com.donututils.realworld.aichat.tool.ServerContextService;
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
 * Client for Google's Gemini API (generativelanguage.googleapis.com). Plain chat only for now -
 * Gemini's function-calling wire format differs enough from Anthropic's that it needs its own
 * verified implementation later, so StockMarket/MarketWatch lookups currently only work with the
 * Anthropic provider.
 */
public final class GeminiApiClient implements ChatClient {

    private static final String ENDPOINT_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    private final Plugin plugin;
    private final HttpClient client;

    public GeminiApiClient(Plugin plugin) {
        this.plugin = plugin;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Override
    public CompletableFuture<Reply> ask(String apiKey, String model, int maxTokens, String systemPrompt,
                                         List<ConversationMemory.Message> history, String userMessage,
                                         ServerContextService context, CommandSender sender) {
        List<Object> contents = new ArrayList<>();
        for (ConversationMemory.Message message : history) {
            contents.add(turn(geminiRole(message.role()), message.content()));
        }
        contents.add(turn("user", userMessage));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", mapOf("parts", List.of(mapOf("text", systemPrompt))));
        body.put("contents", contents);
        body.put("generationConfig", mapOf("maxOutputTokens", (double) maxTokens));

        String json = MiniJson.write(body);
        String url = String.format(ENDPOINT_TEMPLATE, model);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .handle(this::handleResponse);
    }

    @SuppressWarnings("unchecked")
    private Reply handleResponse(HttpResponse<String> response, Throwable error) {
        if (error != null) {
            plugin.getLogger().log(Level.WARNING, "Failed to reach the Gemini API", error);
            return new Reply(false, "Couldn't reach the AI service - check the server console.");
        }
        if (response.statusCode() >= 300) {
            String message = extractErrorMessage(response.body());
            plugin.getLogger().warning("Gemini API returned HTTP " + response.statusCode() + ": " + response.body());
            return new Reply(false, message != null ? message : "The AI service returned an error (HTTP " + response.statusCode() + ").");
        }
        try {
            Map<String, Object> parsed = (Map<String, Object>) MiniJson.parse(response.body());
            List<Object> candidates = (List<Object>) parsed.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return new Reply(false, "The AI service returned no response - it may have blocked the reply for safety reasons.");
            }
            Map<String, Object> firstCandidate = (Map<String, Object>) candidates.get(0);
            Map<String, Object> contentMap = (Map<String, Object>) firstCandidate.get("content");
            List<Object> parts = contentMap == null ? null : (List<Object>) contentMap.get("parts");
            StringBuilder text = new StringBuilder();
            if (parts != null) {
                for (Object partObj : parts) {
                    Map<String, Object> part = (Map<String, Object>) partObj;
                    Object partText = part.get("text");
                    if (partText != null) {
                        if (text.length() > 0) {
                            text.append('\n');
                        }
                        text.append(partText);
                    }
                }
            }
            if (text.length() == 0) {
                return new Reply(false, "The AI service returned an unexpected response.");
            }
            return new Reply(true, text.toString());
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, "Couldn't parse the Gemini API response", ex);
            return new Reply(false, "The AI service returned an unexpected response.");
        }
    }

    @SuppressWarnings("unchecked")
    private static String extractErrorMessage(String body) {
        try {
            Map<String, Object> parsed = (Map<String, Object>) MiniJson.parse(body);
            Object errorObj = parsed.get("error");
            if (errorObj instanceof Map<?, ?> errorMap) {
                Object message = errorMap.get("message");
                return message == null ? null : String.valueOf(message);
            }
        } catch (RuntimeException ignored) {
            // fall through
        }
        return null;
    }

    private static String geminiRole(String role) {
        return "assistant".equals(role) ? "model" : "user";
    }

    private static Map<String, Object> turn(String role, String text) {
        return mapOf("role", role, "parts", List.of(mapOf("text", text)));
    }

    private static Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }
}
