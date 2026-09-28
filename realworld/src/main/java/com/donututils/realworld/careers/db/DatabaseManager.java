package com.donututils.realworld.careers.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/** HikariCP-pooled SQLite connection + schema migration, same pattern as this repo's other plugins. */
public final class DatabaseManager {

    private final Logger logger;
    private final HikariDataSource dataSource;

    public DatabaseManager(File dataFolder, Logger logger) {
        this.logger = logger;
        File dbFile = new File(dataFolder, "careers.db");
        dataFolder.mkdirs();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setMaximumPoolSize(4);
        config.setPoolName("Careers-SQLite");
        config.setConnectionTestQuery("SELECT 1");
        this.dataSource = new HikariDataSource(config);

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL;");
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Failed to set SQLite PRAGMAs", ex);
        }

        migrate();
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void shutdown() {
        dataSource.close();
    }

    private void migrate() {
        String sql = """
                CREATE TABLE IF NOT EXISTS citizens (
                    player_id TEXT PRIMARY KEY,
                    age_tier TEXT,
                    job_id TEXT,
                    last_wage_at INTEGER NOT NULL DEFAULT 0,
                    playtime_minutes INTEGER NOT NULL DEFAULT 0,
                    legacy_count INTEGER NOT NULL DEFAULT 0
                );
                """;
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to migrate Careers' database schema", ex);
        }
    }
}
