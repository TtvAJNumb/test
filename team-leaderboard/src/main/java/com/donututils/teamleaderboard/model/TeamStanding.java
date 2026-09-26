package com.donututils.teamleaderboard.model;

import java.util.UUID;

public record TeamStanding(
        String teamName,
        UUID leaderUuid,
        int memberCount,
        double totalMoney,
        long totalShards
) {
}
