package com.donututils.donutrep.staff;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks which players are frozen by staff (via /sus). A frozen player's movement is blocked by
 * {@link FreezeListener}; command use is not restricted (unlike Municipal's jail) since freezing is a
 * short "stay put while staff look at you" hold, not a punishment. */
public final class FreezeManager {

    private final Set<UUID> frozen = ConcurrentHashMap.newKeySet();

    public boolean isFrozen(UUID playerId) {
        return frozen.contains(playerId);
    }

    /** Returns the new frozen state (true = now frozen). */
    public boolean toggle(UUID playerId) {
        if (frozen.remove(playerId)) {
            return false;
        }
        frozen.add(playerId);
        return true;
    }

    public void unfreeze(UUID playerId) {
        frozen.remove(playerId);
    }
}
