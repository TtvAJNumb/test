package com.donututils.donutrep.punishhistory;

import com.donututils.donutrep.punishhistory.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class NoteStore {

    public record Note(long id, String authorName, String text, long createdAtMillis) {
    }

    private final DatabaseManager database;
    private final Logger logger;

    public NoteStore(DatabaseManager database, Logger logger) {
        this.database = database;
        this.logger = logger;
    }

    public List<Note> notesFor(UUID targetId) {
        List<Note> notes = new ArrayList<>();
        String sql = "SELECT id, author_name, text, created_at FROM staff_notes WHERE target_id = ? ORDER BY created_at DESC";
        try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, targetId.toString());
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    notes.add(new Note(rows.getLong("id"), rows.getString("author_name"), rows.getString("text"), rows.getLong("created_at")));
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Failed to load staff notes for " + targetId, ex);
        }
        return notes;
    }

    public void add(UUID targetId, String authorName, String text) {
        String sql = "INSERT INTO staff_notes (target_id, author_name, text, created_at) VALUES (?, ?, ?, ?)";
        try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, targetId.toString());
            statement.setString(2, authorName);
            statement.setString(3, text);
            statement.setLong(4, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Failed to add staff note for " + targetId, ex);
        }
    }

    /** 1-based index into notesFor(targetId)'s order, matching what /note list shows. */
    public boolean removeByIndex(UUID targetId, int index) {
        List<Note> notes = notesFor(targetId);
        if (index < 1 || index > notes.size()) {
            return false;
        }
        long id = notes.get(index - 1).id();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM staff_notes WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
            return true;
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Failed to remove staff note " + id, ex);
            return false;
        }
    }
}
