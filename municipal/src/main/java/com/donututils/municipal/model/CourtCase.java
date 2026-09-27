package com.donututils.municipal.model;

import java.util.UUID;

public final class CourtCase {

    public enum Status {
        PENDING, CONVICTED, DISMISSED
    }

    private final long id;
    private final UUID defendantId;
    private final UUID officerId;
    private final String reason;
    private Status status;
    private int sentenceMinutes;
    private String verdictReason;
    private UUID judgeId;
    private final long createdAtMillis;
    private Long resolvedAtMillis;

    public CourtCase(long id, UUID defendantId, UUID officerId, String reason, long createdAtMillis) {
        this.id = id;
        this.defendantId = defendantId;
        this.officerId = officerId;
        this.reason = reason;
        this.status = Status.PENDING;
        this.createdAtMillis = createdAtMillis;
    }

    public long id() {
        return id;
    }

    public UUID defendantId() {
        return defendantId;
    }

    public UUID officerId() {
        return officerId;
    }

    public String reason() {
        return reason;
    }

    public Status status() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int sentenceMinutes() {
        return sentenceMinutes;
    }

    public void setSentenceMinutes(int sentenceMinutes) {
        this.sentenceMinutes = sentenceMinutes;
    }

    public String verdictReason() {
        return verdictReason;
    }

    public void setVerdictReason(String verdictReason) {
        this.verdictReason = verdictReason;
    }

    public UUID judgeId() {
        return judgeId;
    }

    public void setJudgeId(UUID judgeId) {
        this.judgeId = judgeId;
    }

    public long createdAtMillis() {
        return createdAtMillis;
    }

    public Long resolvedAtMillis() {
        return resolvedAtMillis;
    }

    public void setResolvedAtMillis(Long resolvedAtMillis) {
        this.resolvedAtMillis = resolvedAtMillis;
    }
}
