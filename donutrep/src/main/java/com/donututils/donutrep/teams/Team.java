package com.donututils.donutrep.teams;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class Team {
    private final long id;
    private final String name;
    private UUID leaderId;
    private final Set<UUID> memberIds = new LinkedHashSet<>();

    public Team(long id, String name, UUID leaderId) {
        this.id = id;
        this.name = name;
        this.leaderId = leaderId;
        this.memberIds.add(leaderId);
    }

    public long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public UUID leaderId() {
        return leaderId;
    }

    public void setLeaderId(UUID leaderId) {
        this.leaderId = leaderId;
    }

    public Set<UUID> memberIds() {
        return memberIds;
    }
}
