package com.donututils.aichat.config;

/** Immutable snapshot of config.yml, re-read on every reload. */
public record AIChatConfig(
        String provider,
        String apiKey,
        String model,
        String ollamaBaseUrl,
        String assistantName,
        String systemPrompt,
        int maxTokens,
        int memoryLimit,
        int cooldownSeconds,
        int maxMessageLength
) {
}
