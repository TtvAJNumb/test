package com.donututils.donutrep.social;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Per-player chat channel: GLOBAL reaches the whole server, LOCAL only players within
 * local-chat-radius-blocks (see config.yml). Purely an in-memory session toggle - everyone starts
 * each join in GLOBAL, matching how most SMPs default. */
public final class ChatManager {

    public enum Channel { GLOBAL, LOCAL }

    private final Map<UUID, Channel> channels = new ConcurrentHashMap<>();

    public Channel channelOf(UUID playerId) {
        return channels.getOrDefault(playerId, Channel.GLOBAL);
    }

    public void setChannel(UUID playerId, Channel channel) {
        channels.put(playerId, channel);
    }

    public void clear(UUID playerId) {
        channels.remove(playerId);
    }
}
