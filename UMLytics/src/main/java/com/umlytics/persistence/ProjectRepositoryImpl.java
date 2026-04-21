package com.umlytics.persistence;

import com.umlytics.domain.Project;
import com.umlytics.interfaces.IProjectRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * SQLite implementation of IProjectRepository.
 * GRASP: Pure Fabrication, Low Coupling
 */
public class ProjectRepositoryImpl implements IProjectRepository {

    private final DatabaseManager db = DatabaseManager.getInstance();
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void save(Project p) {
        if (p == null) throw new IllegalArgumentException("Project cannot be null");
        String sql = "INSERT INTO projects (name, description, created_date, last_modified) VALUES (?,?,?,?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getDescription());
            ps.setString(3, SDF.format(p.getCreatedDate() != null ? p.getCreatedDate() : new Date()));
            ps.setString(4, SDF.format(new Date()));
            ps.executeUpdate();
            try (Statement stmt = conn.createStatement();
                 ResultSet keys = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (keys.next()) p.setProjectId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save project: " + e.getMessage(), e);
        }
    }

    @Override
    public Project findById(int id) {
        String sql = "SELECT * FROM projects WHERE project_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find project: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<Project> findAll() {
        List<Project> list = new ArrayList<>();
        String sql = "SELECT * FROM projects ORDER BY last_modified DESC";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch projects: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM projects WHERE project_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete project: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Project p) {
        if (p == null) throw new IllegalArgumentException("Project cannot be null");
        String sql = "UPDATE projects SET name=?, description=?, last_modified=? WHERE project_id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getDescription());
            ps.setString(3, SDF.format(new Date()));
            ps.setInt(4, p.getProjectId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update project: " + e.getMessage(), e);
        }
    }

    private Project mapRow(ResultSet rs) throws SQLException {
        Project p = new Project();
        p.setProjectId(rs.getInt("project_id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        try {
            p.setCreatedDate(SDF.parse(rs.getString("created_date")));
            p.setLastModifiedDate(SDF.parse(rs.getString("last_modified")));
        } catch (Exception ignored) {}
        return p;
    }
}
