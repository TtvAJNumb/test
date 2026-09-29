package com.donututils.donutrep.travel;

public record RtpConfig(
        double minRadius,
        double maxRadius,
        int cooldownSeconds,
        int maxAttempts,
        int maxConcurrentSearches,
        int queueIntervalSeconds
) {
}
