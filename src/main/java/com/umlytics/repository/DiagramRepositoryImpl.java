package com.umlytics.repository;

import com.umlytics.db.DatabaseManager;
import com.umlytics.domain.UMLDiagram;
import com.umlytics.enums.SourceType;
import com.umlytics.exceptions.DatabaseException;
import com.umlytics.interfaces.IDiagramRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// GRASP: Pure Fabrication, Low Coupling
public class DiagramRepositoryImpl implements IDiagramRepository {
    public DiagramRepositoryImpl() {
    }

    @Override
    public void save(UMLDiagram d) {
        String sql = "INSERT INTO uml_diagram(diagram_id, project_id, name, source_type, render_state, last_updated) VALUES (?, ?, ?, ?, ?, ?)";
        if (d.getDiagramId() == null) d.setDiagramId(UUID.randomUUID());
        if (d.getCreatedDate() == null) d.setCreatedDate(LocalDateTime.now());
        if (d.getLastModifiedDate() == null) d.setLastModifiedDate(LocalDateTime.now());
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            ps.setString(1, d.getDiagramId().toString());
            ps.setString(2, d.getProjectId() == null ? null : d.getProjectId().toString());
            ps.setString(3, d.getTitle() == null ? "Untitled Diagram" : d.getTitle());
            ps.setString(4, d.getSourceType() == null ? SourceType.MANUAL.name() : d.getSourceType().name());
            ps.setString(5, d.getRenderState() == null ? "PENDING" : d.getRenderState().name());
            ps.setString(6, LocalDateTime.now().toString());
            ps.executeUpdate();
            conn.commit();
        } catch (Exception e) {
            throw new DatabaseException("Failed to save diagram.", e);
        }
    }

    @Override
    public UMLDiagram findById(UUID id) {
        String sql = "SELECT * FROM uml_diagram WHERE diagram_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapDiagram(rs);
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to find diagram by id.", e);
        }
    }

    @Override
    public List<UMLDiagram> findByProject(UUID projectId) {
        String sql = "SELECT * FROM uml_diagram WHERE project_id = ? ORDER BY last_updated DESC";
        List<UMLDiagram> diagrams = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projectId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    diagrams.add(mapDiagram(rs));
                }
            }
            return diagrams;
        } catch (Exception e) {
            throw new DatabaseException("Failed to list project diagrams.", e);
        }
    }

    @Override
    public void delete(UUID id) {
        String sql = "DELETE FROM uml_diagram WHERE diagram_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            ps.setString(1, id.toString());
            ps.executeUpdate();
            conn.commit();
        } catch (Exception e) {
            throw new DatabaseException("Failed to delete diagram.", e);
        }
    }

    @Override
    public void update(UMLDiagram d) {
        String sql = "UPDATE uml_diagram SET name = ?, source_type = ?, render_state = ?, last_updated = ? WHERE diagram_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            ps.setString(1, d.getTitle() == null ? "Untitled Diagram" : d.getTitle());
            ps.setString(2, d.getSourceType() == null ? SourceType.MANUAL.name() : d.getSourceType().name());
            ps.setString(3, d.getRenderState() == null ? "PENDING" : d.getRenderState().name());
            ps.setString(4, LocalDateTime.now().toString());
            ps.setString(5, d.getDiagramId().toString());
            ps.executeUpdate();
            conn.commit();
        } catch (Exception e) {
            throw new DatabaseException("Failed to update diagram.", e);
        }
    }

    private UMLDiagram mapDiagram(ResultSet rs) throws Exception {
        UMLDiagram diagram = new UMLDiagram();
        diagram.setDiagramId(UUID.fromString(rs.getString("diagram_id")));
        String projectId = rs.getString("project_id");
        if (projectId != null && !projectId.isBlank()) {
            diagram.setProjectId(UUID.fromString(projectId));
        }
        diagram.setTitle(rs.getString("name"));
        diagram.setSourceType(SourceType.valueOf(rs.getString("source_type")));
        diagram.setLastModifiedDate(LocalDateTime.parse(rs.getString("last_updated")));
        diagram.setCreatedDate(diagram.getLastModifiedDate());
        return diagram;
    }
}
