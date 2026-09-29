package com.donututils.donutrep.social;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Global chat administration, matching real UDS's /chat: mute/unmute the whole server's chat,
 * set a per-player slow-mode delay, or clear the screen. A staff tool, not a per-player channel
 * switch - purely in-memory session state. */
public final class GlobalChatManager {

    private volatile boolean muted = false;
    private volatile int delaySeconds = 0;
    private final Map<UUID, Long> lastMessageAtMillis = new ConcurrentHashMap<>();

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public int delaySeconds() {
        return delaySeconds;
    }

    public void setDelaySeconds(int delaySeconds) {
        this.delaySeconds = Math.max(0, delaySeconds);
    }

    /** True if this player's slow-mode delay has elapsed (and records this attempt as their
     * latest if so). Always true when no delay is configured. */
    public boolean checkAndRecordDelay(UUID playerId) {
        if (delaySeconds <= 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        Long last = lastMessageAtMillis.get(playerId);
        if (last != null && now - last < delaySeconds * 1000L) {
            return false;
        }
        lastMessageAtMillis.put(playerId, now);
        return true;
    }
}
