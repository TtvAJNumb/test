package com.donututils.motors.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/** HikariCP-pooled SQLite connection + schema migration, same pattern as Ledger's and Municipal's
 * DatabaseManager. */
public final class DatabaseManager {

    private final Logger logger;
    private final HikariDataSource dataSource;

    public DatabaseManager(File dataFolder, Logger logger) {
        this.logger = logger;
        File dbFile = new File(dataFolder, "motors.db");
        dataFolder.mkdirs();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setMaximumPoolSize(4);
        config.setPoolName("Motors-SQLite");
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
        String sql = """
                CREATE TABLE IF NOT EXISTS vehicles (
                    entity_uuid TEXT PRIMARY KEY,
                    owner_id TEXT NOT NULL,
                    vehicle_id TEXT NOT NULL,
                    body_uuid TEXT,
                    fuel REAL NOT NULL,
                    wear REAL NOT NULL,
                    mileage REAL NOT NULL,
                    world TEXT NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    z REAL NOT NULL,
                    created_at INTEGER NOT NULL
                );
                """;

        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to migrate Motors' database schema", ex);
        }
    }
}
