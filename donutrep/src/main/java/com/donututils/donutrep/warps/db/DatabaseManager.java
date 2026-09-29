package com.donututils.donutrep.warps.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Owns the SQLite connection pool (HikariCP) and schema for named admin-set warps and the
 * physical world portals that link to them (see PortalManager). */
public final class DatabaseManager {

    private final Logger logger;
    private HikariDataSource dataSource;

    public DatabaseManager(File dataFolder, Logger logger) {
        this.logger = logger;
        File dbFile = new File(dataFolder, "warps.db");
        dataFolder.mkdirs();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setMaximumPoolSize(4);
        config.setPoolName("Warps-SQLite");
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
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private void migrate() {
        String[] statements = {
                """
                CREATE TABLE IF NOT EXISTS warps (
                    name TEXT PRIMARY KEY,
                    world TEXT NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    z REAL NOT NULL,
                    yaw REAL NOT NULL DEFAULT 0,
                    pitch REAL NOT NULL DEFAULT 0
                );
                """,
                """
                CREATE TABLE IF NOT EXISTS portals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    world TEXT NOT NULL,
                    min_x REAL NOT NULL,
                    min_y REAL NOT NULL,
                    min_z REAL NOT NULL,
                    max_x REAL NOT NULL,
                    max_y REAL NOT NULL,
                    max_z REAL NOT NULL,
                    destination_warp TEXT NOT NULL
                );
                """
        };
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to migrate Warps' database schema", ex);
        }
    }
}
