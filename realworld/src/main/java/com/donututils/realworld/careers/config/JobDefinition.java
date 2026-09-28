package com.donututils.realworld.careers.config;

import java.util.Map;

/** One configured job. {@code blockBonuses} pays extra for breaking a matching block (miner ores,
 * lumberjack logs, farmer crops, mason stone); {@code fishCatchBonus}/{@code breedBonus} pay a flat
 * amount per real vanilla catch/breed event. Jobs with none of those (mechanic, police, attorney,
 * medic, executive, stockbroker, landlord, logistics) are passive-wage-only until a custom event
 * bridge into the plugin that owns their real action exists. */
public record JobDefinition(
        String id,
        String displayName,
        AgeTier minAgeTier,
        double wageAmount,
        int wageIntervalMinutes,
        Map<String, Double> blockBonuses,
        double fishCatchBonus,
        double breedBonus
) {
    public boolean availableTo(AgeTier tier) {
        return tier == AgeTier.ADULT || minAgeTier == AgeTier.MINOR;
    }
}
