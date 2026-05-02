package com.umlytics.repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.umlytics.db.DatabaseManager;
import com.umlytics.domain.EvaluationReport;
import com.umlytics.exceptions.DatabaseException;
import com.umlytics.interfaces.IEvaluationRepository;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// GRASP: Pure Fabrication
public class EvaluationRepositoryImpl implements IEvaluationRepository {
    private static final Gson GSON = new Gson();
    private static final Type LIST_OF_STRING = new TypeToken<List<String>>() { }.getType();
    private final Connection connection;

    public EvaluationRepositoryImpl() {
        this.connection = null;
    }

    @Override
    public void save(EvaluationReport r) {
        String sql = "INSERT INTO evaluation_reports(diagram_id, project_id, coupling_score, cohesion_score, solid_score, suggestions, generated_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
        if (r.getGeneratedDate() == null) {
            r.setGeneratedDate(new Date());
        }
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            conn.setAutoCommit(false);
            ps.setInt(1, r.getDiagramId());
            ps.setInt(2, r.getProjectId());
            ps.setDouble(3, r.getCouplingScore());
            ps.setDouble(4, r.getCohesionScore());
            ps.setDouble(5, r.getSolidScore());
            ps.setString(6, GSON.toJson(r.getSuggestions()));
            ps.setString(7, r.getGeneratedDate().toInstant().toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    r.setReportId(rs.getInt(1));
                }
            }
            conn.commit();
        } catch (Exception e) {
            throw new DatabaseException("Failed to save evaluation report.", e);
        }
    }

    @Override
    public List<EvaluationReport> findByProject(int pid) {
        String sql = "SELECT * FROM evaluation_reports WHERE project_id = ? ORDER BY generated_date DESC";
        List<EvaluationReport> reports = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reports.add(mapReport(rs));
                }
            }
            return reports;
        } catch (Exception e) {
            throw new DatabaseException("Failed to fetch evaluation reports by project.", e);
        }
    }

    @Override
    public EvaluationReport findByDiagram(int did) {
        String sql = "SELECT * FROM evaluation_reports WHERE diagram_id = ? ORDER BY generated_date DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, did);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapReport(rs);
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to fetch evaluation report by diagram.", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM evaluation_reports WHERE report_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            ps.setInt(1, id);
            ps.executeUpdate();
            conn.commit();
        } catch (Exception e) {
            throw new DatabaseException("Failed to delete evaluation report.", e);
        }
    }

    private EvaluationReport mapReport(ResultSet rs) throws Exception {
        EvaluationReport report = new EvaluationReport();
        report.setReportId(rs.getInt("report_id"));
        report.setDiagramId(rs.getInt("diagram_id"));
        report.setProjectId(rs.getInt("project_id"));
        report.setCouplingScore(rs.getDouble("coupling_score"));
        report.setCohesionScore(rs.getDouble("cohesion_score"));
        report.setSolidScore(rs.getDouble("solid_score"));
        String suggestionsJson = rs.getString("suggestions");
        if (suggestionsJson != null && !suggestionsJson.isBlank()) {
            report.setSuggestions(GSON.fromJson(suggestionsJson, LIST_OF_STRING));
        }
        report.setGeneratedDate(Date.from(Instant.parse(rs.getString("generated_date"))));
        return report;
    }
}
