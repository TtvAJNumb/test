package com.donututils.realworld.aichat.memory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps a short, in-memory rolling window of each player's recent exchanges with the assistant, so
 * it has short-term context without needing any persistence. Cleared on server restart. */
public final class ConversationMemory {

    public record Message(String role, String content) {
    }

    private final Map<UUID, Deque<Message>> history = new ConcurrentHashMap<>();

    public List<Message> get(UUID playerId) {
        Deque<Message> deque = history.get(playerId);
        if (deque == null) {
            return List.of();
        }
        synchronized (deque) {
            return List.copyOf(deque);
        }
    }

    public void record(UUID playerId, String userMessage, String assistantReply, int memoryLimit) {
        Deque<Message> deque = history.computeIfAbsent(playerId, id -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addLast(new Message("user", userMessage));
            deque.addLast(new Message("assistant", assistantReply));
            int maxEntries = Math.max(1, memoryLimit) * 2;
            while (deque.size() > maxEntries) {
                deque.pollFirst();
            }
        }
    }

    public void reset(UUID playerId) {
        history.remove(playerId);
    }
}
