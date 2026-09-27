package com.hostel.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * DBConnection — Central JDBC connection factory.
 *
 * Design decisions:
 *  - Reads ALL credentials from db.properties (never hard-coded here)
 *  - Single shared Connection per application run (desktop app = one user)
 *  - Reconnects automatically if the connection drops or times out
 *  - Provides beginTransaction / commit / rollback helpers for service layer
 *  - testConnection() used by the Phase 3 test runner and by the Login screen
 */
public class DBConnection {

    // ── Singleton connection (package-private for testing) ─────────────────
    private static Connection connection = null;

    // ── Credentials loaded once from db.properties ─────────────────────────
    private static String dbUrl;
    private static String dbUsername;
    private static String dbPassword;
    private static String dbDriver;

    // Static initializer: load properties when class is first used
    static {
        loadProperties();
    }

    // ── Constructor private: this is a utility class ───────────────────────
    private DBConnection() {}

    // ── Load credentials from classpath resource ───────────────────────────

    /**
     * Reads db.url, db.username, db.password, db.driver from db.properties.
     * db.properties must be in src/main/resources/ (Maven copies it to classpath).
     */
    private static void loadProperties() {
        Properties props = new Properties();

        // 1. Check external configuration file in config/db.properties or db.properties
        java.io.File externalConfigFile = new java.io.File("config/db.properties");
        if (!externalConfigFile.exists()) {
            externalConfigFile = new java.io.File("db.properties");
        }

        if (externalConfigFile.exists()) {
            try (InputStream in = new java.io.FileInputStream(externalConfigFile)) {
                props.load(in);
                System.out.println("[DBConnection] Loaded external config from: " + externalConfigFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("[DBConnection] Failed to load external config file: " + e.getMessage());
            }
        }

        // 2. Fallback to classpath db.properties if external file was not present or empty
        if (props.isEmpty()) {
            try (InputStream in = DBConnection.class
                    .getClassLoader()
                    .getResourceAsStream("db.properties")) {

                if (in == null) {
                    throw new RuntimeException(
                        "db.properties not found in external path (config/db.properties) or classpath.\n" +
                        "Please ensure config/db.properties or src/main/resources/db.properties exists.");
                }

                props.load(in);
                System.out.println("[DBConnection] Loaded configuration from classpath db.properties");
            } catch (IOException e) {
                throw new RuntimeException("Could not read db.properties from classpath: " + e.getMessage(), e);
            }
        }

        dbUrl      = props.getProperty("db.url");
        dbUsername = props.getProperty("db.username");
        dbPassword = props.getProperty("db.password");
        dbDriver   = props.getProperty("db.driver");

        // Validate that all four keys are present
        if (dbUrl == null || dbUsername == null || dbPassword == null || dbDriver == null) {
            throw new RuntimeException(
                "db.properties is missing one or more required keys:\n" +
                "  db.url, db.username, db.password, db.driver");
        }
    }

    // ── Get connection ─────────────────────────────────────────────────────

    /**
     * Returns the shared JDBC Connection.
     * Creates or reconnects as needed.
     *
     * @throws SQLException if the connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        // Reconnect if: null, closed, or no longer valid (network timeout)
        if (connection == null || connection.isClosed() || !connection.isValid(3)) {
            connect();
        }
        return connection;
    }

    /**
     * Internal: establishes the JDBC connection using loaded properties.
     */
    private static void connect() throws SQLException {
        try {
            Class.forName(dbDriver);
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                "MySQL JDBC driver class not found: " + dbDriver + "\n" +
                "Check that mysql-connector-j is in pom.xml dependencies.", e);
        }
        connection = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
        System.out.println("[DBConnection] Connected to: " + dbUrl);
    }

    // ── Test connectivity ──────────────────────────────────────────────────

    /**
     * Attempts to get a connection and validates it.
     * Safe to call from UI (does not throw — returns boolean).
     *
     * @return true if the database is reachable, false otherwise
     */
    public static boolean testConnection() {
        try {
            Connection conn = getConnection();
            return conn != null && !conn.isClosed() && conn.isValid(3);
        } catch (SQLException e) {
            System.err.println("[DBConnection] Connection test failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns a human-readable connection summary for diagnostics.
     * Used by the Phase 3 test runner.
     */
    public static String getConnectionInfo() {
        try {
            Connection conn = getConnection();
            DatabaseMetaData meta = conn.getMetaData();
            return String.format(
                "  Host    : %s%n" +
                "  DB      : %s%n" +
                "  Product : %s %s%n" +
                "  Driver  : %s",
                meta.getURL(),
                conn.getCatalog(),
                meta.getDatabaseProductName(),
                meta.getDatabaseProductVersion(),
                meta.getDriverName()
            );
        } catch (SQLException e) {
            return "  [Could not retrieve connection info: " + e.getMessage() + "]";
        }
    }

    // ── Transaction helpers (used by service layer in Phase 4+) ───────────

    /**
     * Starts a database transaction.
     * After calling this, you MUST call either commit() or rollback().
     */
    public static void beginTransaction() throws SQLException {
        getConnection().setAutoCommit(false);
    }

    /**
     * Commits the current transaction and restores auto-commit mode.
     */
    public static void commitTransaction() throws SQLException {
        Connection conn = getConnection();
        conn.commit();
        conn.setAutoCommit(true);
    }

    /**
     * Rolls back the current transaction (called in catch blocks).
     * Restores auto-commit mode. Never throws — safe to call in finally.
     */
    public static void rollbackTransaction() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.rollback();
                connection.setAutoCommit(true);
                System.err.println("[DBConnection] Transaction rolled back.");
            }
        } catch (SQLException e) {
            System.err.println("[DBConnection] Rollback failed: " + e.getMessage());
        }
    }

    // ── Close on application exit ──────────────────────────────────────────

    /**
     * Closes the shared connection.
     * Call this when the application shuts down.
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    System.out.println("[DBConnection] Connection closed.");
                }
            } catch (SQLException e) {
                System.err.println("[DBConnection] Error closing connection: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }
}
