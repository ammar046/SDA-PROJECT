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
            // Non-destructive migration path from legacy plural/integer schema.
            statement.execute("""
                INSERT OR IGNORE INTO project(project_id, name, description, created_date, last_modified)
                SELECT CAST(project_id AS TEXT), name, description, created_date, last_modified_date
                FROM projects
                """);
        } catch (Exception ignored) {
            // Legacy table may not exist.
        }
        try {
            statement.execute("""
                INSERT OR IGNORE INTO uml_diagram(diagram_id, project_id, name, source_type, render_state, last_updated)
                SELECT CAST(diagram_id AS TEXT), CAST(project_id AS TEXT), title,
                       CASE source_type
                            WHEN 'NL' THEN 'NATURAL_LANGUAGE'
                            WHEN 'CODE' THEN 'SOURCE_CODE'
                            WHEN 'UPLOAD' THEN 'UPLOADED_IMAGE'
                            ELSE source_type
                       END,
                       'PENDING',
                       last_modified_date
                FROM uml_diagrams
                """);
        } catch (Exception ignored) {
            // Legacy table may not exist.
        }
        try {
            statement.execute("""
                INSERT OR IGNORE INTO conceptual_class(class_id, diagram_id, class_name, class_type, visibility, position_x, position_y, header_color, border_color, member_font_size, class_width, class_height)
                SELECT CAST(class_id AS TEXT), CAST(diagram_id AS TEXT), name,
                       CASE WHEN is_interface = 1 THEN 'INTERFACE'
                            WHEN is_abstract = 1 THEN 'ABSTRACT'
                            ELSE 'ENTITY' END,
                       'PUBLIC',
                       position_x, position_y,
                       COALESCE(header_color, 'Blue'),
                       COALESCE(border_color, 'Blue'),
                       COALESCE(member_font_size, 12.0),
                       COALESCE(class_width, 200),
                       COALESCE(class_height, 140)
                FROM uml_classes
                """);
        } catch (Exception ignored) {
            // Legacy table may not exist.
        }
        try {
            statement.execute("""
                INSERT OR IGNORE INTO attribute(attribute_id, class_id, attribute_name, data_type, default_value, visibility)
                SELECT CAST(attribute_id AS TEXT), CAST(class_id AS TEXT), name, type, NULL, visibility
                FROM attributes
                """);
        } catch (Exception ignored) {
            // Legacy table may not exist.
        }
        try {
            statement.execute("""
                INSERT OR IGNORE INTO method(method_id, class_id, method_name, return_type, parameters, visibility)
                SELECT CAST(method_id AS TEXT), CAST(class_id AS TEXT), name, return_type, parameters, visibility
                FROM methods
                """);
        } catch (Exception ignored) {
            // Legacy table may not exist.
        }
        try {
            statement.execute("""
                INSERT OR IGNORE INTO relationship(relationship_id, diagram_id, relationship_type, source_class_id, target_class_id, source_multiplicity, target_multiplicity, label, bend_x, edge_color, dashed)
                SELECT CAST(relationship_id AS TEXT), CAST(diagram_id AS TEXT), relationship_type,
                       CAST(source_class_id AS TEXT), CAST(target_class_id AS TEXT),
                       source_multiplicity, target_multiplicity, label, bend_x,
                       COALESCE(edge_color, 'Black'), COALESCE(dashed, 0)
                FROM relationships
                """);
        } catch (Exception ignored) {
            // Legacy table may not exist.
        }
        try {
            statement.execute("""
                INSERT OR IGNORE INTO chat_message(message_id, project_id, class_id, sender_role, content, timestamp)
                SELECT CAST(message_id AS TEXT), CAST(project_id AS TEXT), NULL, sender, content, timestamp
                FROM chat_messages
                """);
        } catch (Exception ignored) {
            // Legacy table may not exist.
        }
        try {
            statement.execute("""
                INSERT OR IGNORE INTO design_evaluation_report(report_id, diagram_id, project_id, coupling_score, cohesion_score, solid_score, feedback_summary, evaluation_date)
                SELECT CAST(report_id AS TEXT), CAST(diagram_id AS TEXT), CAST(project_id AS TEXT),
                       coupling_score, cohesion_score, solid_score, suggestions, generated_date
                FROM evaluation_reports
                """);
        } catch (Exception ignored) {
            // Legacy table may not exist.
        }
        try {
            statement.execute("""
                INSERT OR IGNORE INTO class_suggestion(suggestion_id, class_id, diagram_id, skeleton_code, explanation, accepted)
                SELECT CAST(suggestion_id AS TEXT), NULL, CAST(diagram_id AS TEXT), code_skeletons, NULL, 0
                FROM structure_suggestions
                """);
        } catch (Exception ignored) {
            // Legacy table may not exist.
        }
    }
}
