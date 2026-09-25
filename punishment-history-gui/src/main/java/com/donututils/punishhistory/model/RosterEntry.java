package com.donututils.punishhistory.model;

import java.util.UUID;

/** One row of the roster menu: a player plus a quick summary of why they're in it. */
public record RosterEntry(
        UUID uuid,
        String name,
        int punishmentCount,
        int noteCount,
        long lastActivity
) {
}
