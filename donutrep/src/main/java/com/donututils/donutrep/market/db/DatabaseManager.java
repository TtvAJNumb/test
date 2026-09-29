package com.donututils.donutrep.market.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Owns the SQLite connection pool (HikariCP) and schema for Market's sell history/leaderboard
 * ledger - the catalog and dynamic prices themselves stay in market-prices.yml, this is just the
 * transaction log that /sellhistory, /sellprogress, and /topsell read from. */
public final class DatabaseManager {

    private final Logger logger;
    private HikariDataSource dataSource;

    public DatabaseManager(File dataFolder, Logger logger) {
        this.logger = logger;
        File dbFile = new File(dataFolder, "market.db");
        dataFolder.mkdirs();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setMaximumPoolSize(4);
        config.setPoolName("Market-SQLite");
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
        String sql = """
                CREATE TABLE IF NOT EXISTS sell_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_id TEXT NOT NULL,
                    player_name TEXT NOT NULL,
                    material TEXT NOT NULL,
                    amount INTEGER NOT NULL,
                    total_price REAL NOT NULL,
                    sold_at INTEGER NOT NULL
                );
                """;
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to migrate Market's database schema", ex);
        }
    }
}
