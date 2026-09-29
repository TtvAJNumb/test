package com.donututils.realworld.purchasealert.config;

public record AlertConfig(
        String backendUrl,
        String pluginKey,
        int pollIntervalSeconds,
        int orderLimit,
        String webhookUrl,
        String webhookUsername
) {
}
