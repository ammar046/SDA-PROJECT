package com.umlytics.persistence;

import com.umlytics.domain.EvaluationReport;
import com.umlytics.interfaces.IEvaluationRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
 * SQLite implementation of IEvaluationRepository.
 * GRASP: Pure Fabrication
 */
public class EvaluationRepositoryImpl implements IEvaluationRepository {

    private final DatabaseManager db  = DatabaseManager.getInstance();
    private final Gson            gson = new Gson();
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void save(EvaluationReport r) {
        if (r == null) throw new IllegalArgumentException("EvaluationReport cannot be null");
        String sql = "INSERT INTO evaluation_reports (diagram_id, project_id, coupling_score, cohesion_score, solid_score, suggestions, generated_date) VALUES (?,?,?,?,?,?,?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, r.getDiagramId());
            ps.setInt(2, r.getProjectId());
            ps.setDouble(3, r.getCouplingScore());
            ps.setDouble(4, r.getCohesionScore());
            ps.setDouble(5, r.getSolidScore());
            ps.setString(6, gson.toJson(r.getSuggestions()));
            ps.setString(7, SDF.format(r.getGeneratedDate() != null ? r.getGeneratedDate() : new Date()));
            ps.executeUpdate();
            try (Statement stmt = conn.createStatement();
                 ResultSet keys = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (keys.next()) r.setReportId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save evaluation report: " + e.getMessage(), e);
        }
    }

    @Override
    public List<EvaluationReport> findByProject(int projectId) {
        List<EvaluationReport> list = new ArrayList<>();
        String sql = "SELECT * FROM evaluation_reports WHERE project_id=? ORDER BY generated_date DESC";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load evaluation reports: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public EvaluationReport findByDiagram(int diagramId) {
        String sql = "SELECT * FROM evaluation_reports WHERE diagram_id=? ORDER BY generated_date DESC LIMIT 1";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, diagramId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find evaluation report: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM evaluation_reports WHERE report_id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete evaluation report: " + e.getMessage(), e);
        }
    }

    private EvaluationReport mapRow(ResultSet rs) throws SQLException {
        EvaluationReport r = new EvaluationReport();
        r.setReportId(rs.getInt("report_id"));
        r.setDiagramId(rs.getInt("diagram_id"));
        r.setProjectId(rs.getInt("project_id"));
        r.setCouplingScore(rs.getDouble("coupling_score"));
        r.setCohesionScore(rs.getDouble("cohesion_score"));
        r.setSolidScore(rs.getDouble("solid_score"));
        List<String> suggestions = gson.fromJson(
                rs.getString("suggestions"),
                new TypeToken<List<String>>(){}.getType());
        r.setSuggestions(suggestions != null ? suggestions : new ArrayList<>());
        try {
            r.setGeneratedDate(SDF.parse(rs.getString("generated_date")));
        } catch (Exception ignored) {}
        return r;
    }
}
