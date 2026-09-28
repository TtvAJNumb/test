package com.donututils.realworld.municipal.court;

import com.donututils.realworld.municipal.db.DatabaseManager;
import com.donututils.realworld.municipal.model.CourtCase;
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
 * Arrests, trials, and verdicts. Every case, resolved or not, stays in the in-memory cache and the
 * database as a permanent criminal record.
 * <p>
 * Thread contract: {@link #arrest} blocks on a database insert to get the new case's id back, and
 * {@link #sentence}/{@link #dismiss} block on a database update to confirm it before jailing anyone -
 * callers MUST invoke all three from an async task, never directly from a command handler.
 */
public final class CourtManager {

    public record VerdictResult(boolean success, String message, CourtCase courtCase) {
        static VerdictResult fail(String message) {
            return new VerdictResult(false, message, null);
        }
    }

    private final Plugin plugin;
    private final DatabaseManager database;

    private final Map<Long, CourtCase> cases = new ConcurrentHashMap<>();
    private final Map<UUID, List<Long>> casesByDefendant = new ConcurrentHashMap<>();

    public CourtManager(Plugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        loadAll();
    }

    private void loadAll() {
        String sql = "SELECT id, defendant_id, officer_id, reason, status, sentence_minutes, verdict_reason, judge_id, created_at, resolved_at FROM cases";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                try {
                    CourtCase courtCase = new CourtCase(rows.getLong("id"), UUID.fromString(rows.getString("defendant_id")),
                            UUID.fromString(rows.getString("officer_id")), rows.getString("reason"), rows.getLong("created_at"));
                    courtCase.setStatus(CourtCase.Status.valueOf(rows.getString("status")));
                    courtCase.setSentenceMinutes(rows.getInt("sentence_minutes"));
                    courtCase.setVerdictReason(rows.getString("verdict_reason"));
                    String judgeId = rows.getString("judge_id");
                    if (judgeId != null) {
                        courtCase.setJudgeId(UUID.fromString(judgeId));
                    }
                    long resolvedAt = rows.getLong("resolved_at");
                    if (!rows.wasNull()) {
                        courtCase.setResolvedAtMillis(resolvedAt);
                    }
                    index(courtCase);
                } catch (IllegalArgumentException ignored) {
                    // skip malformed row
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load court cases", ex);
        }
    }

    private void index(CourtCase courtCase) {
        cases.put(courtCase.id(), courtCase);
        casesByDefendant.computeIfAbsent(courtCase.defendantId(), id -> new ArrayList<>()).add(courtCase.id());
    }

    public CourtCase getCase(long id) {
        return cases.get(id);
    }

    public List<CourtCase> recordFor(UUID playerId) {
        List<CourtCase> result = new ArrayList<>();
        for (Long id : casesByDefendant.getOrDefault(playerId, List.of())) {
            CourtCase courtCase = cases.get(id);
            if (courtCase != null) {
                result.add(courtCase);
            }
        }
        return result;
    }

    public CourtCase arrest(UUID officerId, UUID defendantId, String reason) {
        long now = System.currentTimeMillis();
        CourtCase courtCase = new CourtCase(-1, defendantId, officerId, reason, now);
        String sql = "INSERT INTO cases (defendant_id, officer_id, reason, status, sentence_minutes, created_at) VALUES (?, ?, ?, 'PENDING', 0, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, defendantId.toString());
            statement.setString(2, officerId.toString());
            statement.setString(3, reason);
            statement.setLong(4, now);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    CourtCase persisted = new CourtCase(keys.getLong(1), defendantId, officerId, reason, now);
                    index(persisted);
                    return persisted;
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to record arrest", ex);
        }
        return null;
    }

    public VerdictResult sentence(UUID judgeId, long caseId, int minutes, String verdictReason, int maxSentenceMinutes) {
        CourtCase courtCase = cases.get(caseId);
        if (courtCase == null) {
            return VerdictResult.fail("No case #" + caseId + " found.");
        }
        if (courtCase.status() != CourtCase.Status.PENDING) {
            return VerdictResult.fail("Case #" + caseId + " has already been resolved.");
        }
        int clampedMinutes = Math.max(1, Math.min(minutes, maxSentenceMinutes));
        courtCase.setStatus(CourtCase.Status.CONVICTED);
        courtCase.setSentenceMinutes(clampedMinutes);
        courtCase.setVerdictReason(verdictReason);
        courtCase.setJudgeId(judgeId);
        courtCase.setResolvedAtMillis(System.currentTimeMillis());
        persistVerdict(courtCase);
        return new VerdictResult(true, "Sentenced to " + clampedMinutes + " minute(s).", courtCase);
    }

    public VerdictResult dismiss(UUID judgeId, long caseId, String reason) {
        CourtCase courtCase = cases.get(caseId);
        if (courtCase == null) {
            return VerdictResult.fail("No case #" + caseId + " found.");
        }
        if (courtCase.status() != CourtCase.Status.PENDING) {
            return VerdictResult.fail("Case #" + caseId + " has already been resolved.");
        }
        courtCase.setStatus(CourtCase.Status.DISMISSED);
        courtCase.setVerdictReason(reason);
        courtCase.setJudgeId(judgeId);
        courtCase.setResolvedAtMillis(System.currentTimeMillis());
        persistVerdict(courtCase);
        return new VerdictResult(true, "Case #" + caseId + " dismissed.", courtCase);
    }

    private void persistVerdict(CourtCase courtCase) {
        String sql = "UPDATE cases SET status = ?, sentence_minutes = ?, verdict_reason = ?, judge_id = ?, resolved_at = ? WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, courtCase.status().name());
            statement.setInt(2, courtCase.sentenceMinutes());
            statement.setString(3, courtCase.verdictReason());
            statement.setString(4, courtCase.judgeId() == null ? null : courtCase.judgeId().toString());
            statement.setLong(5, courtCase.resolvedAtMillis());
            statement.setLong(6, courtCase.id());
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to persist verdict for case #" + courtCase.id(), ex);
        }
    }
}
