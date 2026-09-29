package com.donututils.donutrep.teams;

import com.donututils.donutrep.teams.db.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Teams: a bounded, from-scratch replacement for the membership half of the real UltimateDonutSmp's
 * much bigger /team (which also has homes, team chat, and PvP toggles - none of that is rebuilt here,
 * only enough that TeamLeaderboard's money/shards rankings mean something again). A player is in at
 * most one team; the founder is its leader and the only one who can add/remove members or disband it.
 */
public final class TeamManager {

    private final Plugin plugin;
    private final DatabaseManager database;

    private final Map<Long, Team> teamsById = new ConcurrentHashMap<>();
    private final Map<UUID, Long> teamIdByPlayer = new ConcurrentHashMap<>();

    public TeamManager(Plugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        loadAll();
    }

    private void loadAll() {
        try (Connection connection = database.getConnection(); Statement statement = connection.createStatement()) {
            try (ResultSet rows = statement.executeQuery("SELECT id, name, leader_id FROM teams")) {
                while (rows.next()) {
                    Team team = new Team(rows.getLong("id"), rows.getString("name"), UUID.fromString(rows.getString("leader_id")));
                    team.memberIds().clear();
                    teamsById.put(team.id(), team);
                }
            }
            try (ResultSet rows = statement.executeQuery("SELECT player_id, team_id FROM team_members")) {
                while (rows.next()) {
                    UUID playerId = UUID.fromString(rows.getString("player_id"));
                    long teamId = rows.getLong("team_id");
                    Team team = teamsById.get(teamId);
                    if (team != null) {
                        team.memberIds().add(playerId);
                        teamIdByPlayer.put(playerId, teamId);
                    }
                }
            }
        } catch (SQLException | IllegalArgumentException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load teams", ex);
        }
        plugin.getLogger().info("Loaded " + teamsById.size() + " team(s).");
    }

    public Team teamOf(UUID playerId) {
        Long teamId = teamIdByPlayer.get(playerId);
        return teamId == null ? null : teamsById.get(teamId);
    }

    public Team teamByName(String name) {
        for (Team team : teamsById.values()) {
            if (team.name().equalsIgnoreCase(name)) {
                return team;
            }
        }
        return null;
    }

    public List<Team> allTeams() {
        return new ArrayList<>(teamsById.values());
    }

    public enum Result { OK, ALREADY_IN_TEAM, NAME_TAKEN, NOT_FOUND, NOT_LEADER, NOT_A_MEMBER, TARGET_IN_TEAM }

    public Result createTeam(UUID leaderId, String name) {
        if (teamIdByPlayer.containsKey(leaderId)) {
            return Result.ALREADY_IN_TEAM;
        }
        if (teamByName(name) != null) {
            return Result.NAME_TAKEN;
        }
        long id = insertTeam(name, leaderId);
        if (id < 0) {
            return Result.NAME_TAKEN;
        }
        Team team = new Team(id, name, leaderId);
        teamsById.put(id, team);
        teamIdByPlayer.put(leaderId, id);
        persistMemberAsync(leaderId, id);
        return Result.OK;
    }

    public Result disband(UUID leaderId) {
        Team team = teamOf(leaderId);
        if (team == null) {
            return Result.NOT_A_MEMBER;
        }
        if (!team.leaderId().equals(leaderId)) {
            return Result.NOT_LEADER;
        }
        for (UUID member : List.copyOf(team.memberIds())) {
            teamIdByPlayer.remove(member);
        }
        teamsById.remove(team.id());
        deleteTeamAsync(team.id());
        return Result.OK;
    }

    public Result addMember(UUID leaderId, UUID targetId) {
        Team team = teamOf(leaderId);
        if (team == null) {
            return Result.NOT_A_MEMBER;
        }
        if (!team.leaderId().equals(leaderId)) {
            return Result.NOT_LEADER;
        }
        if (teamIdByPlayer.containsKey(targetId)) {
            return Result.TARGET_IN_TEAM;
        }
        team.memberIds().add(targetId);
        teamIdByPlayer.put(targetId, team.id());
        persistMemberAsync(targetId, team.id());
        return Result.OK;
    }

    public Result removeMember(UUID leaderId, UUID targetId) {
        Team team = teamOf(leaderId);
        if (team == null) {
            return Result.NOT_A_MEMBER;
        }
        if (!team.leaderId().equals(leaderId)) {
            return Result.NOT_LEADER;
        }
        if (targetId.equals(leaderId) || !team.memberIds().remove(targetId)) {
            return Result.NOT_FOUND;
        }
        teamIdByPlayer.remove(targetId);
        deleteMemberAsync(targetId);
        return Result.OK;
    }

    public Result leave(UUID playerId) {
        Team team = teamOf(playerId);
        if (team == null) {
            return Result.NOT_A_MEMBER;
        }
        if (team.leaderId().equals(playerId)) {
            return disband(playerId);
        }
        team.memberIds().remove(playerId);
        teamIdByPlayer.remove(playerId);
        deleteMemberAsync(playerId);
        return Result.OK;
    }

    private long insertTeam(String name, UUID leaderId) {
        String sql = "INSERT INTO teams (name, leader_id, created_at) VALUES (?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setString(2, leaderId.toString());
            statement.setLong(3, System.currentTimeMillis());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create team " + name, ex);
            return -1;
        }
    }

    private void persistMemberAsync(UUID playerId, long teamId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO team_members (player_id, team_id) VALUES (?, ?) "
                    + "ON CONFLICT(player_id) DO UPDATE SET team_id = excluded.team_id";
            try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerId.toString());
                statement.setLong(2, teamId);
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to persist team membership for " + playerId, ex);
            }
        });
    }

    private void deleteMemberAsync(UUID playerId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM team_members WHERE player_id = ?")) {
                statement.setString(1, playerId.toString());
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to delete team membership for " + playerId, ex);
            }
        });
    }

    private void deleteTeamAsync(long teamId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = database.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM teams WHERE id = ?")) {
                    statement.setLong(1, teamId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM team_members WHERE team_id = ?")) {
                    statement.setLong(1, teamId);
                    statement.executeUpdate();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to delete team " + teamId, ex);
            }
        });
    }
}
