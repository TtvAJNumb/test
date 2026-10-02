package com.donututils.donutrep.staff;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GodManager {

    private final Set<UUID> godMode = ConcurrentHashMap.newKeySet();

    public boolean isGod(UUID playerId) {
        return godMode.contains(playerId);
    }

    public boolean toggle(UUID playerId) {
        if (godMode.remove(playerId)) {
            return false;
        }
        godMode.add(playerId);
        return true;
    }
}
