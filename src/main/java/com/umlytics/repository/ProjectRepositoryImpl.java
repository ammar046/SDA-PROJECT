package com.umlytics.repository;

import com.umlytics.db.DatabaseManager;
import com.umlytics.domain.Project;
import com.umlytics.exceptions.DatabaseException;
import com.umlytics.interfaces.IProjectRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// GRASP: Pure Fabrication, Low Coupling
public class ProjectRepositoryImpl implements IProjectRepository {
    public ProjectRepositoryImpl() {
    }

    @Override
    public void save(Project p) {
        String sql = "INSERT INTO projects(name, description, created_date, last_modified_date) VALUES (?, ?, ?, ?)";
        Date now = new Date();
        if (p.getCreatedDate() == null) {
            p.setCreatedDate(now);
        }
        if (p.getLastModifiedDate() == null) {
            p.setLastModifiedDate(now);
        }
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, p.getName());
                ps.setString(2, p.getDescription());
                ps.setString(3, p.getCreatedDate().toInstant().toString());
                ps.setString(4, p.getLastModifiedDate().toInstant().toString());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        p.setProjectId(rs.getInt(1));
                    }
                }
                conn.commit();
            } catch (Exception inner) {
                conn.rollback();
                throw inner;
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to save project.", e);
        }
    }

    @Override
    public Project findById(int id) {
        String sql = "SELECT * FROM projects WHERE project_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapProject(rs);
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to find project by id.", e);
        }
    }

    @Override
    public List<Project> findAll() {
        String sql = "SELECT * FROM projects ORDER BY last_modified_date DESC";
        List<Project> projects = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                projects.add(mapProject(rs));
            }
            return projects;
        } catch (Exception e) {
            throw new DatabaseException("Failed to list projects.", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM projects WHERE project_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
                conn.commit();
            } catch (Exception inner) {
                conn.rollback();
                throw inner;
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to delete project.", e);
        }
    }

    @Override
    public void update(Project p) {
        String sql = "UPDATE projects SET name = ?, description = ?, last_modified_date = ? WHERE project_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getName());
                ps.setString(2, p.getDescription());
                ps.setString(3, Instant.now().toString());
                ps.setInt(4, p.getProjectId());
                ps.executeUpdate();
                conn.commit();
            } catch (Exception inner) {
                conn.rollback();
                throw inner;
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to update project.", e);
        }
    }

    private Project mapProject(ResultSet rs) throws Exception {
        Project project = new Project();
        project.setProjectId(rs.getInt("project_id"));
        project.setName(rs.getString("name"));
        project.setDescription(rs.getString("description"));
        project.setCreatedDate(Date.from(Instant.parse(rs.getString("created_date"))));
        project.setLastModifiedDate(Date.from(Instant.parse(rs.getString("last_modified_date"))));
        return project;
    }
}
