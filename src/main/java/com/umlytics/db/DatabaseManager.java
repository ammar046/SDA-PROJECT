package com.umlytics.db;

import com.umlytics.exceptions.DatabaseException;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

// GRASP: Pure Fabrication
public class DatabaseManager {
    private static DatabaseManager instance;
    private final ConnectionPool connectionPool;
    private String jdbcUrl;
    private String username;
    private String password;

    private DatabaseManager() {
        this.connectionPool = new ConnectionPool();
        loadConfig();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public synchronized void initialize() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            String schemaSql = loadSchemaSql();
            for (String part : schemaSql.split(";")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
            applySchemaMigrations(statement);
            connection.commit();
        } catch (Exception e) {
            throw new DatabaseException("Failed to initialize database schema.", e);
        }
    }

    public Connection getConnection() {
        try {
            Connection pooled = connectionPool.borrow();
            if (pooled != null && !pooled.isClosed()) {
                return pooled;
            }
            if (username == null || username.isBlank()) {
                return DriverManager.getConnection(jdbcUrl);
            }
            return DriverManager.getConnection(jdbcUrl, username, password);
        } catch (Exception e) {
            throw new DatabaseException("Failed to acquire database connection.", e);
        }
    }

    public void closeConnection(Connection c) {
        try {
            if (c == null || c.isClosed()) {
                return;
            }
            connectionPool.release(c);
        } catch (Exception e) {
            throw new DatabaseException("Failed to close database connection.", e);
        }
    }

    public ResultSet executeQuery(String sql) {
        try {
            Connection connection = getConnection();
            Statement statement = connection.createStatement();
            return statement.executeQuery(sql);
        } catch (Exception e) {
            throw new DatabaseException("Failed to execute query.", e);
        }
    }

    public int executeUpdate(String sql) {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            int rows = statement.executeUpdate(sql);
            connection.commit();
            return rows;
        } catch (Exception e) {
            throw new DatabaseException("Failed to execute update.", e);
        }
    }

    private void loadConfig() {
        Properties properties = new Properties();
        try (InputStream in = DatabaseManager.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (Exception ignored) {
            // Defaults are applied below.
        }
        this.jdbcUrl = properties.getProperty("db.url", "jdbc:sqlite:umlytics.db");
        this.username = properties.getProperty("db.username", "");
        this.password = properties.getProperty("db.password", "");
    }

    private String loadSchemaSql() {
        try (InputStream in = DatabaseManager.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (in == null) {
                throw new DatabaseException("schema.sql not found in resources.", null);
            }
            return new String(in.readAllBytes());
        } catch (Exception e) {
            throw new DatabaseException("Failed to read schema SQL.", e);
        }
    }

    private void applySchemaMigrations(Statement statement) {
        try {
            statement.execute("ALTER TABLE relationships ADD COLUMN bend_x REAL");
        } catch (Exception ignored) {
            // Column already exists.
        }
        try {
            statement.execute("ALTER TABLE relationships ADD COLUMN edge_color TEXT DEFAULT 'Black'");
        } catch (Exception ignored) {
            // Column already exists.
        }
        try {
            statement.execute("ALTER TABLE relationships ADD COLUMN dashed INTEGER DEFAULT 0");
        } catch (Exception ignored) {
            // Column already exists.
        }
        try {
            statement.execute("ALTER TABLE uml_classes ADD COLUMN header_color TEXT DEFAULT 'Blue'");
        } catch (Exception ignored) {
            // Column already exists.
        }
        try {
            statement.execute("ALTER TABLE uml_classes ADD COLUMN border_color TEXT DEFAULT 'Blue'");
        } catch (Exception ignored) {
            // Column already exists.
        }
        try {
            statement.execute("ALTER TABLE uml_classes ADD COLUMN member_font_size REAL DEFAULT 12");
        } catch (Exception ignored) {
            // Column already exists.
        }
    }
}
