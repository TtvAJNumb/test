package com.donututils.realworld.careers.model;

import com.donututils.realworld.careers.config.AgeTier;

import java.util.UUID;

public final class CitizenProfile {

    private final UUID playerId;
    private AgeTier ageTier;
    private String jobId;
    private long lastWageAtMillis;

    public CitizenProfile(UUID playerId, AgeTier ageTier, String jobId, long lastWageAtMillis) {
        this.playerId = playerId;
        this.ageTier = ageTier;
        this.jobId = jobId;
        this.lastWageAtMillis = lastWageAtMillis;
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
