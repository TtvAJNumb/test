package com.donututils.realworld.municipal.model;

import java.util.UUID;

public record JailRecord(UUID playerId, long caseId, long jailedUntilMillis,
                          String originWorld, double originX, double originY, double originZ) {
}
