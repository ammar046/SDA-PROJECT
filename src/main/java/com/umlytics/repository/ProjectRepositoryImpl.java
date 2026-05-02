package com.umlytics.repository;

import com.umlytics.db.DatabaseManager;
import com.umlytics.domain.Project;
import com.umlytics.exceptions.DatabaseException;
import com.umlytics.interfaces.IProjectRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// GRASP: Pure Fabrication, Low Coupling
public class ProjectRepositoryImpl implements IProjectRepository {
    public ProjectRepositoryImpl() {
    }

    @Override
    public void save(Project p) {
        String sql = "INSERT INTO project(project_id, name, description, created_date, last_modified) VALUES (?, ?, ?, ?, ?)";
        LocalDateTime now = LocalDateTime.now();
        if (p.getProjectId() == null) p.setProjectId(UUID.randomUUID());
        if (p.getCreatedDate() == null) p.setCreatedDate(now);
        if (p.getLastModifiedDate() == null) p.setLastModifiedDate(now);
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getProjectId().toString());
                ps.setString(2, p.getName());
                ps.setString(3, p.getDescription());
                ps.setString(4, p.getCreatedDate().toString());
                ps.setString(5, p.getLastModifiedDate().toString());
                ps.executeUpdate();
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
    public Project findById(UUID id) {
        String sql = "SELECT * FROM project WHERE project_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.toString());
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
        String sql = "SELECT * FROM project ORDER BY last_modified DESC";
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
    public void delete(UUID id) {
        String sql = "DELETE FROM project WHERE project_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id.toString());
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
        String sql = "UPDATE project SET name = ?, description = ?, last_modified = ? WHERE project_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getName());
                ps.setString(2, p.getDescription());
                ps.setString(3, LocalDateTime.now().toString());
                ps.setString(4, p.getProjectId().toString());
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
        project.setProjectId(UUID.fromString(rs.getString("project_id")));
        project.setName(rs.getString("name"));
        project.setDescription(rs.getString("description"));
        project.setCreatedDate(LocalDateTime.parse(rs.getString("created_date")));
        project.setLastModifiedDate(LocalDateTime.parse(rs.getString("last_modified")));
        return project;
    }
}
