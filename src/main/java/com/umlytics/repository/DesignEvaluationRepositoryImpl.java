package com.umlytics.repository;

import com.umlytics.db.DatabaseManager;
import com.umlytics.domain.DesignEvaluationReport;
import com.umlytics.exceptions.DatabaseException;
import com.umlytics.interfaces.IEvaluationRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// GRASP: Pure Fabrication
public class DesignEvaluationRepositoryImpl implements IEvaluationRepository {
    @Override
    public void save(DesignEvaluationReport r) {
        String sql = "INSERT INTO design_evaluation_report(report_id, diagram_id, project_id, coupling_score, cohesion_score, solid_score, feedback_summary, evaluation_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        if (r.getReportId() == null) r.setReportId(UUID.randomUUID());
        if (r.getEvaluationDate() == null) r.setEvaluationDate(LocalDateTime.now());
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            ps.setString(1, r.getReportId().toString());
            ps.setString(2, r.getDiagramId() == null ? null : r.getDiagramId().toString());
            ps.setString(3, r.getProjectId() == null ? null : r.getProjectId().toString());
            ps.setFloat(4, r.getCouplingScore());
            ps.setFloat(5, r.getCohesionScore());
            ps.setFloat(6, r.getSolidScore());
            ps.setString(7, r.getFeedbackSummary());
            ps.setString(8, r.getEvaluationDate().toString());
            ps.executeUpdate();
            conn.commit();
        } catch (Exception e) {
            throw new DatabaseException("Failed to save evaluation report.", e);
        }
    }

    @Override
    public List<DesignEvaluationReport> findByProject(UUID projectId) {
        String sql = "SELECT * FROM design_evaluation_report WHERE project_id = ? ORDER BY evaluation_date DESC";
        List<DesignEvaluationReport> reports = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projectId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) reports.add(mapReport(rs));
            }
            return reports;
        } catch (Exception e) {
            throw new DatabaseException("Failed to fetch evaluation reports by project.", e);
        }
    }

    @Override
    public DesignEvaluationReport findByDiagram(UUID diagramId) {
        String sql = "SELECT * FROM design_evaluation_report WHERE diagram_id = ? ORDER BY evaluation_date DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, diagramId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapReport(rs);
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to fetch evaluation report by diagram.", e);
        }
    }

    @Override
    public void delete(UUID id) {
        String sql = "DELETE FROM design_evaluation_report WHERE report_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            ps.setString(1, id.toString());
            ps.executeUpdate();
            conn.commit();
        } catch (Exception e) {
            throw new DatabaseException("Failed to delete evaluation report.", e);
        }
    }

    private DesignEvaluationReport mapReport(ResultSet rs) throws Exception {
        DesignEvaluationReport report = new DesignEvaluationReport();
        report.setReportId(UUID.fromString(rs.getString("report_id")));
        String diagramId = rs.getString("diagram_id");
        if (diagramId != null && !diagramId.isBlank()) report.setDiagramId(UUID.fromString(diagramId));
        String projectId = rs.getString("project_id");
        if (projectId != null && !projectId.isBlank()) report.setProjectId(UUID.fromString(projectId));
        report.setCouplingScore(rs.getFloat("coupling_score"));
        report.setCohesionScore(rs.getFloat("cohesion_score"));
        report.setSolidScore(rs.getFloat("solid_score"));
        report.setFeedbackSummary(rs.getString("feedback_summary"));
        report.setEvaluationDate(LocalDateTime.parse(rs.getString("evaluation_date")));
        return report;
    }
}
