package com.umlytics.persistence;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.stream.Collectors;

/**
 * Singleton database manager that provides SQLite connections.
 * GRASP: Pure Fabrication, Singleton (GoF)
 */
public class DatabaseManager {

    private static DatabaseManager instance;

    private static final String DB_PATH = "database/umlytics.db";
    private final String jdbcUrl;
    private final String username = "";

    private DatabaseManager() {
        jdbcUrl = "jdbc:sqlite:" + DB_PATH;
        initializeDatabase();
    }

    /** GoF Singleton: thread-safe lazy initialization */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(jdbcUrl);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get DB connection: " + e.getMessage(), e);
        }
    }

    public void closeConnection(Connection c) {
        if (c != null) {
            try { c.close(); } catch (SQLException ignored) {}
        }
    }

    public ResultSet executeQuery(String sql) {
        try {
            Connection conn = getConnection();
            Statement stmt  = conn.createStatement();
            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + e.getMessage(), e);
        }
    }

    public int executeUpdate(String sql) {
        Connection conn = getConnection();
        try (Statement stmt = conn.createStatement()) {
            int result = stmt.executeUpdate(sql);
            conn.close();
            return result;
        } catch (SQLException e) {
            closeConnection(conn);
            throw new RuntimeException("Update failed: " + e.getMessage(), e);
        }
    }

    private void initializeDatabase() {
        try {
            // Ensure database directory exists
            Files.createDirectories(Paths.get("database"));

            // Load schema from resources
            InputStream in = getClass().getResourceAsStream("/db/schema.sql");
            if (in == null) {
                System.err.println("Warning: schema.sql not found in resources. DB may be empty.");
                return;
            }
            String schema = new BufferedReader(new InputStreamReader(in))
                    .lines().collect(Collectors.joining("\n"));

            // Execute each statement separately
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                for (String sql : schema.split(";")) {
                    String trimmed = sql.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
                closeConnection(conn);
            }
        } catch (Exception e) {
            System.err.println("Database initialization error: " + e.getMessage());
        }
    }
}
