package com.donututils.donutrep.staff;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks who has "always staffchat" toggled on (every chat line of theirs routes to staff chat instead
 * of public chat) - separate from one-off /staffchat <message> broadcasts. */
public final class StaffChatManager {

    private final Set<UUID> alwaysOn = ConcurrentHashMap.newKeySet();

    public boolean isAlwaysOn(UUID playerId) {
        return alwaysOn.contains(playerId);
    }

    public boolean toggleAlwaysOn(UUID playerId) {
        if (alwaysOn.remove(playerId)) {
            return false;
        }
        alwaysOn.add(playerId);
        return true;
    }
}
