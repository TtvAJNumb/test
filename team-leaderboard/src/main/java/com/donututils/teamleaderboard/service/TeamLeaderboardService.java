package com.donututils.teamleaderboard.service;

import com.donututils.teamleaderboard.config.TeamLeaderboardConfig;
import com.donututils.teamleaderboard.model.StatKind;
import com.donututils.teamleaderboard.model.TeamStanding;
import com.donututils.teamleaderboard.reflect.UdsBridge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sums each team's members' money/shards off UltimateDonutSmp's leaderboard data and caches the
 * ranked result briefly, so repeated page clicks in the same few seconds don't re-walk every team.
 */
public final class TeamLeaderboardService {

    private final UdsBridge bridge;
    private volatile TeamLeaderboardConfig config;
    private final Map<StatKind, CachedStandings> cache = new EnumMap<>(StatKind.class);

    private record CachedStandings(long computedAtMillis, List<TeamStanding> standings) {
    }

    public TeamLeaderboardService(UdsBridge bridge, TeamLeaderboardConfig config) {
        this.bridge = bridge;
        this.config = config;
    }

    public void updateConfig(TeamLeaderboardConfig config) {
        this.config = config;
        cache.clear();
    }

    public List<TeamStanding> getStandings(StatKind kind) {
        CachedStandings cached = cache.get(kind);
        long now = System.currentTimeMillis();
        long ttlMillis = Math.max(1, config.refreshIntervalSeconds()) * 1000L;
        if (cached != null && now - cached.computedAtMillis() < ttlMillis) {
            return cached.standings();
        }

        List<TeamStanding> standings = computeStandings(kind);
        cache.put(kind, new CachedStandings(now, standings));
        return standings;
    }

    public TeamStanding findTeam(List<TeamStanding> standings, String teamName) {
        for (TeamStanding standing : standings) {
            if (standing.teamName().equalsIgnoreCase(teamName)) {
                return standing;
            }
        }
        return null;
    }

    private List<TeamStanding> computeStandings(StatKind kind) {
        List<UdsBridge.TeamInfo> teams = bridge.getTeams();
        Map<UUID, Double> money = kind == StatKind.MONEY ? bridge.getMoneyByPlayer() : Map.of();
        Map<UUID, Long> shards = kind == StatKind.SHARDS ? bridge.getShardsByPlayer() : Map.of();

        List<TeamStanding> standings = new ArrayList<>();
        for (UdsBridge.TeamInfo team : teams) {
            double totalMoney = 0;
            long totalShards = 0;
            for (UUID member : team.memberUuids()) {
                totalMoney += money.getOrDefault(member, 0.0);
                totalShards += shards.getOrDefault(member, 0L);
            }
            standings.add(new TeamStanding(team.name(), team.leaderUuid(), team.memberCount(), totalMoney, totalShards));
        }

        Comparator<TeamStanding> comparator = kind == StatKind.MONEY
                ? Comparator.comparingDouble(TeamStanding::totalMoney).reversed()
                : Comparator.comparingLong(TeamStanding::totalShards).reversed();
        standings.sort(comparator);
        return standings;
    }
}
