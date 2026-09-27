package com.donututils.aichat.config;

/** Immutable snapshot of config.yml, re-read on every reload. */
public record AIChatConfig(
        String apiKey,
        String model,
        String assistantName,
        String systemPrompt,
        int maxTokens,
        int memoryLimit,
        int cooldownSeconds,
        int maxMessageLength
) {
}
