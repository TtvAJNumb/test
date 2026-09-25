package com.donututils.punishhistory.model;

import java.util.UUID;

/** A UltimateDonutSmp PunishmentRecord, flattened out of reflection into plain fields. */
public record PunishmentSnapshot(
        long id,
        UUID targetUuid,
        String targetName,
        String type,          // BAN, MUTE, VOICE_MUTE, WARN, KICK, BLACKLIST
        String displayType,    // as above, or TEMPBAN/TEMPMUTE when time-limited
        String reason,
        String issuerName,
        long issuedAt,
        Long expiresAt,
        boolean removed,
        String removedByName,
        String removalReason,
        String state           // ACTIVE, EXPIRED, REMOVED
) {
}
