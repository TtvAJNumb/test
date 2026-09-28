package com.donututils.realworld.municipal.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/** HikariCP-pooled SQLite connection + schema migration, same pattern as Ledger's DatabaseManager. */
public final class DatabaseManager {

    private final Logger logger;
    private final HikariDataSource dataSource;

    public DatabaseManager(File dataFolder, Logger logger) {
        this.logger = logger;
        File dbFile = new File(dataFolder, "municipal.db");
        dataFolder.mkdirs();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setMaximumPoolSize(4);
        config.setPoolName("Municipal-SQLite");
        config.setConnectionTestQuery("SELECT 1");
        this.dataSource = new HikariDataSource(config);

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL;");
            statement.execute("PRAGMA foreign_keys=ON;");
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
        String[] statements = {
                """
                CREATE TABLE IF NOT EXISTS cases (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    defendant_id TEXT NOT NULL,
                    officer_id TEXT NOT NULL,
                    reason TEXT NOT NULL,
                    status TEXT NOT NULL,
                    sentence_minutes INTEGER NOT NULL DEFAULT 0,
                    verdict_reason TEXT,
                    judge_id TEXT,
                    created_at INTEGER NOT NULL,
                    resolved_at INTEGER
                );
                """,
                """
                CREATE TABLE IF NOT EXISTS jail_records (
                    player_id TEXT PRIMARY KEY,
                    case_id INTEGER NOT NULL,
                    jailed_until INTEGER NOT NULL,
                    origin_world TEXT,
                    origin_x REAL,
                    origin_y REAL,
                    origin_z REAL
                );
                """,
                """
                CREATE TABLE IF NOT EXISTS permits (
                    player_id TEXT NOT NULL,
                    permit_type TEXT NOT NULL,
                    purchased_at INTEGER NOT NULL,
                    PRIMARY KEY (player_id, permit_type)
                );
                """,
                """
                CREATE TABLE IF NOT EXISTS claims (
                    world TEXT NOT NULL,
                    chunk_x INTEGER NOT NULL,
                    chunk_z INTEGER NOT NULL,
                    owner_id TEXT NOT NULL,
                    claimed_at INTEGER NOT NULL,
                    PRIMARY KEY (world, chunk_x, chunk_z)
                );
                """,
                """
                CREATE TABLE IF NOT EXISTS claim_tax_status (
                    owner_id TEXT PRIMARY KEY,
                    missed_ticks INTEGER NOT NULL DEFAULT 0,
                    last_tick_at INTEGER
                );
                """,
                """
                CREATE TABLE IF NOT EXISTS locations (
                    name TEXT PRIMARY KEY,
                    world TEXT NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    z REAL NOT NULL,
                    yaw REAL NOT NULL DEFAULT 0,
                    pitch REAL NOT NULL DEFAULT 0,
                    has_bounds INTEGER NOT NULL DEFAULT 0,
                    min_x REAL, min_y REAL, min_z REAL,
                    max_x REAL, max_y REAL, max_z REAL
                );
                """
        };

        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to migrate Municipal's database schema", ex);
        }
    }
}
