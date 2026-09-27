package com.donututils.ledger.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns the SQLite connection pool (HikariCP) and schema migration. Every table Ledger's managers
 * need lives here, created with {@code IF NOT EXISTS} so re-running on an already-set-up database is
 * a no-op. WAL mode is enabled so reads aren't blocked while a write is in flight - SQLite only
 * allows one writer at a time regardless, so the pool is kept small.
 */
public final class DatabaseManager {

    private final Logger logger;
    private HikariDataSource dataSource;

    public DatabaseManager(File dataFolder, Logger logger) {
        this.logger = logger;
        File dbFile = new File(dataFolder, "ledger.db");
        dataFolder.mkdirs();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setMaximumPoolSize(4);
        config.setPoolName("Ledger-SQLite");
        // SQLite has no real concept of a dedicated connection-test query; this one always exists.
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
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private void migrate() {
        String[] statements = {
                """
                CREATE TABLE IF NOT EXISTS bank_accounts (
                    player_id TEXT NOT NULL,
                    account_type TEXT NOT NULL,
                    balance REAL NOT NULL DEFAULT 0,
                    apy REAL NOT NULL DEFAULT 0,
                    last_interest_at INTEGER NOT NULL,
                    PRIMARY KEY (player_id, account_type)
                );
                """,
                """
                CREATE TABLE IF NOT EXISTS credit_profiles (
                    player_id TEXT PRIMARY KEY,
                    score INTEGER NOT NULL DEFAULT 650,
                    on_time_payments INTEGER NOT NULL DEFAULT 0,
                    missed_payments INTEGER NOT NULL DEFAULT 0,
                    defaults INTEGER NOT NULL DEFAULT 0,
                    last_updated INTEGER NOT NULL
                );
                """,
                """
                CREATE TABLE IF NOT EXISTS loans (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_id TEXT NOT NULL,
                    principal REAL NOT NULL,
                    remaining_balance REAL NOT NULL,
                    apr REAL NOT NULL,
                    term_days INTEGER NOT NULL,
                    issued_at INTEGER NOT NULL,
                    next_payment_due INTEGER NOT NULL,
                    missed_payments INTEGER NOT NULL DEFAULT 0,
                    status TEXT NOT NULL
                );
                """,
                """
                CREATE TABLE IF NOT EXISTS corporations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE NOT NULL,
                    ticker TEXT UNIQUE NOT NULL,
                    founder_id TEXT NOT NULL,
                    treasury_balance REAL NOT NULL DEFAULT 0,
                    total_shares INTEGER NOT NULL,
                    share_price REAL NOT NULL,
                    sector TEXT,
                    founded_at INTEGER NOT NULL,
                    trust_protected INTEGER NOT NULL DEFAULT 0
                );
                """,
                """
                CREATE TABLE IF NOT EXISTS shareholdings (
                    corporation_id INTEGER NOT NULL,
                    player_id TEXT NOT NULL,
                    shares INTEGER NOT NULL,
                    PRIMARY KEY (corporation_id, player_id)
                );
                """,
                """
                CREATE TABLE IF NOT EXISTS corp_financials (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    corporation_id INTEGER NOT NULL,
                    revenue REAL NOT NULL,
                    expenses REAL NOT NULL,
                    reported_by TEXT NOT NULL,
                    reported_at INTEGER NOT NULL
                );
                """,
                """
                CREATE TABLE IF NOT EXISTS share_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    corporation_id INTEGER NOT NULL,
                    player_id TEXT NOT NULL,
                    side TEXT NOT NULL,
                    shares INTEGER NOT NULL,
                    price_per_share REAL NOT NULL,
                    timestamp INTEGER NOT NULL
                );
                """,
                """
                CREATE TABLE IF NOT EXISTS tax_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_id TEXT,
                    corporation_id INTEGER,
                    amount REAL NOT NULL,
                    tax_type TEXT NOT NULL,
                    period INTEGER NOT NULL
                );
                """
        };

        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to migrate Ledger's database schema", ex);
        }
    }
}
