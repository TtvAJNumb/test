package com.donututils.aichat.client;

import com.donututils.aichat.json.JsonUtil;
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
 * Client for the Anthropic Messages API, including a tool-calling loop: if the model asks to call one
 * of {@link ServerContextService}'s read-only tools, this fetches the real answer and feeds it back so
 * the model's final reply is grounded in actual server data instead of a guess. Hand-rolled JSON via
 * {@link MiniJson}, same dependency-free approach used across this repo.
 */
public final class ClaudeApiClient {

    private static final String ENDPOINT = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_TOOL_ROUNDS = 3;

    private final Plugin plugin;
    private final HttpClient client;

    public ClaudeApiClient(Plugin plugin) {
        this.plugin = plugin;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public record Reply(boolean success, String text) {
    }

    public CompletableFuture<Reply> ask(String apiKey, String model, int maxTokens, String systemPrompt,
                                         List<ConversationMemory.Message> history, String userMessage,
                                         ServerContextService context, CommandSender sender) {
        List<Object> messages = new ArrayList<>();
        for (ConversationMemory.Message message : history) {
            messages.add(textMessage(message.role(), message.content()));
        }
        messages.add(textMessage("user", userMessage));

        Map<String, Object> requestTemplate = new LinkedHashMap<>();
        requestTemplate.put("model", model);
        requestTemplate.put("max_tokens", (double) maxTokens);
        requestTemplate.put("system", systemPrompt);
        if (context != null && context.hasTools()) {
            requestTemplate.put("tools", context.toolSchemas());
        }

        return sendRound(apiKey, requestTemplate, messages, context, sender, MAX_TOOL_ROUNDS);
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<Reply> sendRound(String apiKey, Map<String, Object> requestTemplate, List<Object> messages,
                                                ServerContextService context, CommandSender sender, int roundsLeft) {
        Map<String, Object> body = new LinkedHashMap<>(requestTemplate);
        body.put("messages", messages);
        String json = MiniJson.write(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .handle((response, error) -> handleResponse(apiKey, requestTemplate, messages, context, sender, roundsLeft, response, error))
                .thenCompose(future -> future);
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<Reply> handleResponse(String apiKey, Map<String, Object> requestTemplate, List<Object> messages,
                                                     ServerContextService context, CommandSender sender, int roundsLeft,
                                                     HttpResponse<String> response, Throwable error) {
        if (error != null) {
            plugin.getLogger().log(Level.WARNING, "Failed to reach the Claude API", error);
            return CompletableFuture.completedFuture(new Reply(false, "Couldn't reach the AI service - check the server console."));
        }

        if (response.statusCode() >= 300) {
            String message = extractErrorMessage(response.body());
            plugin.getLogger().warning("Claude API returned HTTP " + response.statusCode() + ": " + response.body());
            return CompletableFuture.completedFuture(new Reply(false, message != null ? message : "The AI service returned an error (HTTP " + response.statusCode() + ")."));
        }

        Map<String, Object> responseMap;
        try {
            responseMap = (Map<String, Object>) MiniJson.parse(response.body());
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, "Couldn't parse the Claude API response", ex);
            return CompletableFuture.completedFuture(new Reply(false, "The AI service returned an unexpected response."));
        }

        String stopReason = (String) responseMap.get("stop_reason");
        List<Object> content = (List<Object>) responseMap.get("content");

        if ("tool_use".equals(stopReason) && context != null && roundsLeft > 0 && content != null) {
            List<Object> newMessages = new ArrayList<>(messages);
            newMessages.add(mapOf("role", "assistant", "content", content));

            List<Object> toolResults = new ArrayList<>();
            for (Object blockObj : content) {
                Map<String, Object> block = (Map<String, Object>) blockObj;
                if (!"tool_use".equals(block.get("type"))) {
                    continue;
                }
                String id = (String) block.get("id");
                String name = (String) block.get("name");
                Map<String, Object> input = (Map<String, Object>) block.get("input");
                Map<String, String> args = new LinkedHashMap<>();
                if (input != null) {
                    for (Map.Entry<String, Object> entry : input.entrySet()) {
                        args.put(entry.getKey(), entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
                    }
                }
                String resultText = context.execute(name, args, sender);
                toolResults.add(mapOf("type", "tool_result", "tool_use_id", id, "content", resultText));
            }
            newMessages.add(mapOf("role", "user", "content", toolResults));

            return sendRound(apiKey, requestTemplate, newMessages, context, sender, roundsLeft - 1);
        }

        StringBuilder text = new StringBuilder();
        if (content != null) {
            for (Object blockObj : content) {
                Map<String, Object> block = (Map<String, Object>) blockObj;
                if ("text".equals(block.get("type"))) {
                    if (text.length() > 0) {
                        text.append('\n');
                    }
                    text.append((String) block.get("text"));
                }
            }
        }
        if (text.length() == 0) {
            return CompletableFuture.completedFuture(new Reply(false, "The AI service returned an unexpected response."));
        }
        return CompletableFuture.completedFuture(new Reply(true, text.toString()));
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
        return JsonUtil.extractString(body, "message");
    }

    private static Map<String, Object> textMessage(String role, String text) {
        return mapOf("role", role, "content", List.of(mapOf("type", "text", "text", text)));
    }

    private static Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }
}
