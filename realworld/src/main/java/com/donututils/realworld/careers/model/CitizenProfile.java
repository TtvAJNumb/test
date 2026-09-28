package com.donututils.realworld.careers.model;

import com.donututils.realworld.careers.config.AgeTier;

import java.util.UUID;

public final class CitizenProfile {

    private final UUID playerId;
    private AgeTier ageTier;
    private String jobId;
    private long lastWageAtMillis;
    private int playtimeMinutes;
    private int legacyCount;

    public CitizenProfile(UUID playerId, AgeTier ageTier, String jobId, long lastWageAtMillis) {
        this(playerId, ageTier, jobId, lastWageAtMillis, 0, 0);
    }

    public CitizenProfile(UUID playerId, AgeTier ageTier, String jobId, long lastWageAtMillis,
                           int playtimeMinutes, int legacyCount) {
        this.playerId = playerId;
        this.ageTier = ageTier;
        this.jobId = jobId;
        this.lastWageAtMillis = lastWageAtMillis;
        this.playtimeMinutes = playtimeMinutes;
        this.legacyCount = legacyCount;
    }

    public int playtimeMinutes() {
        return playtimeMinutes;
    }

    public void setPlaytimeMinutes(int playtimeMinutes) {
        this.playtimeMinutes = playtimeMinutes;
    }

    public int legacyCount() {
        return legacyCount;
    }

    public void setLegacyCount(int legacyCount) {
        this.legacyCount = legacyCount;
    }

    public UUID playerId() {
        return playerId;
    }

    public AgeTier ageTier() {
        return ageTier;
    }

    public void setAgeTier(AgeTier ageTier) {
        this.ageTier = ageTier;
    }

    public String jobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public long lastWageAtMillis() {
        return lastWageAtMillis;
    }

    public void setLastWageAtMillis(long lastWageAtMillis) {
        this.lastWageAtMillis = lastWageAtMillis;
    }

    public boolean hasChosenAgeTier() {
        return ageTier != null;
    }

    public boolean hasChosenJob() {
        return jobId != null;
    }
}
