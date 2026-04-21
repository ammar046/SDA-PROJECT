package com.umlytics.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.umlytics.domain.*;
import com.umlytics.domain.relationships.*;
import com.umlytics.enums.*;
import com.umlytics.interfaces.IDiagramRepository;

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
 * SQLite implementation of IDiagramRepository.
 * Persists UMLDiagram along with nested UMLClasses, Attributes, Methods, Relationships.
 * GRASP: Pure Fabrication, Low Coupling
 */
public class DiagramRepositoryImpl implements IDiagramRepository {

    private final DatabaseManager db   = DatabaseManager.getInstance();
    private final Gson            gson = new GsonBuilder().setPrettyPrinting().create();
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void save(UMLDiagram d) {
        if (d == null) throw new IllegalArgumentException("Diagram cannot be null");
        String sql = "INSERT INTO diagrams (project_id, title, source_type, model_json, created_date, last_modified) VALUES (?,?,?,?,?,?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, d.getProjectId());
            ps.setString(2, d.getTitle());
            ps.setString(3, d.getSourceType() != null ? d.getSourceType().name() : SourceType.MANUAL.name());
            ps.setString(4, d.serialize());
            String now = SDF.format(new Date());
            ps.setString(5, now);
            ps.setString(6, now);
            ps.executeUpdate();
            try (Statement stmt = conn.createStatement();
                 ResultSet keys = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (keys.next()) d.setDiagramId(keys.getInt(1));
            }
            saveClasses(conn, d);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save diagram: " + e.getMessage(), e);
        }
    }

    @Override
    public UMLDiagram findById(int id) {
        String sql = "SELECT * FROM diagrams WHERE diagram_id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                UMLDiagram d = mapDiagramRow(rs);
                loadClasses(conn, d);
                loadRelationships(conn, d);
                return d;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find diagram: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<UMLDiagram> findByProject(int projectId) {
        List<UMLDiagram> list = new ArrayList<>();
        String sql = "SELECT * FROM diagrams WHERE project_id=? ORDER BY last_modified DESC";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UMLDiagram d = mapDiagramRow(rs);
                loadClasses(conn, d);
                loadRelationships(conn, d);
                list.add(d);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch diagrams: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM diagrams WHERE diagram_id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete diagram: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(UMLDiagram d) {
        if (d == null) throw new IllegalArgumentException("Diagram cannot be null");
        String sql = "UPDATE diagrams SET title=?, source_type=?, model_json=?, last_modified=? WHERE diagram_id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, d.getTitle());
            ps.setString(2, d.getSourceType() != null ? d.getSourceType().name() : SourceType.MANUAL.name());
            ps.setString(3, d.serialize());
            ps.setString(4, SDF.format(new Date()));
            ps.setInt(5, d.getDiagramId());
            ps.executeUpdate();
            // Re-save classes (delete + re-insert)
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM uml_classes WHERE diagram_id=?")) {
                del.setInt(1, d.getDiagramId());
                del.executeUpdate();
            }
            saveClasses(conn, d);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update diagram: " + e.getMessage(), e);
        }
    }

    // ---- Private helpers ----

    private void saveClasses(Connection conn, UMLDiagram d) throws SQLException {
        String classSql = "INSERT INTO uml_classes (diagram_id, name, is_abstract, is_interface, position_x, position_y) VALUES (?,?,?,?,?,?)";
        for (UMLClass c : d.getClasses()) {
            try (PreparedStatement ps = conn.prepareStatement(classSql)) {
                ps.setInt(1, d.getDiagramId());
                ps.setString(2, c.getName());
                ps.setInt(3, c.isAbstract() ? 1 : 0);
                ps.setInt(4, c.isInterface() ? 1 : 0);
                ps.setDouble(5, c.getPositionX());
                ps.setDouble(6, c.getPositionY());
                ps.executeUpdate();
                try (Statement stmt = conn.createStatement();
                     ResultSet keys = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (keys.next()) c.setClassId(keys.getInt(1));
                }
                saveAttributes(conn, c);
                saveMethods(conn, c);
            }
        }
        saveRelationships(conn, d);
    }

    private void saveAttributes(Connection conn, UMLClass c) throws SQLException {
        String sql = "INSERT INTO attributes (class_id, name, type, visibility, is_static) VALUES (?,?,?,?,?)";
        for (Attribute a : c.getAttributes()) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, c.getClassId());
                ps.setString(2, a.getName());
                ps.setString(3, a.getType());
                ps.setString(4, a.getVisibility() != null ? a.getVisibility().name() : Visibility.PRIVATE.name());
                ps.setInt(5, a.isStatic() ? 1 : 0);
                ps.executeUpdate();
                try (Statement stmt = conn.createStatement();
                     ResultSet keys = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (keys.next()) a.setAttributeId(keys.getInt(1));
                }
            }
        }
    }

    private void saveMethods(Connection conn, UMLClass c) throws SQLException {
        String sql = "INSERT INTO methods (class_id, name, return_type, parameters, visibility, is_abstract) VALUES (?,?,?,?,?,?)";
        for (Method m : c.getMethods()) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, c.getClassId());
                ps.setString(2, m.getName());
                ps.setString(3, m.getReturnType());
                ps.setString(4, String.join(",", m.getParameters()));
                ps.setString(5, m.getVisibility() != null ? m.getVisibility().name() : Visibility.PUBLIC.name());
                ps.setInt(6, m.isAbstract() ? 1 : 0);
                ps.executeUpdate();
                try (Statement stmt = conn.createStatement();
                     ResultSet keys = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (keys.next()) m.setMethodId(keys.getInt(1));
                }
            }
        }
    }

    private void saveRelationships(Connection conn, UMLDiagram d) throws SQLException {
        String sql = "INSERT INTO relationships (diagram_id,source_class_id,target_class_id,rel_type,source_multiplicity,target_multiplicity,label,is_composition,is_aggregation,navigability,dependency_type,is_interface_rel) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        for (Relationship r : d.getRelationships()) {
            if (r.getSourceClass() == null || r.getTargetClass() == null) continue;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, d.getDiagramId());
                ps.setInt(2, r.getSourceClass().getClassId());
                ps.setInt(3, r.getTargetClass().getClassId());
                ps.setString(4, r.getType().name());
                ps.setString(5, r.getSourceMultiplicity() != null ? r.getSourceMultiplicity() : "1");
                ps.setString(6, r.getTargetMultiplicity() != null ? r.getTargetMultiplicity() : "*");
                ps.setString(7, r.getLabel() != null ? r.getLabel() : "");
                ps.setInt(8, (r instanceof AssociationRelationship ar && ar.isComposition()) ? 1 : 0);
                ps.setInt(9, (r instanceof AssociationRelationship ar && ar.isAggregation()) ? 1 : 0);
                ps.setString(10, (r instanceof AssociationRelationship ar && ar.getNavigability() != null) ? ar.getNavigability().name() : "UNIDIRECTIONAL");
                ps.setString(11, (r instanceof DependencyRelationship dr) ? dr.getDependencyType() : "");
                ps.setInt(12, (r instanceof InheritanceRelationship ir && ir.isInterface()) ? 1 : 0);
                ps.executeUpdate();
            }
        }
    }

    private void loadClasses(Connection conn, UMLDiagram d) throws SQLException {
        String sql = "SELECT * FROM uml_classes WHERE diagram_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, d.getDiagramId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UMLClass c = new UMLClass(rs.getInt("class_id"), rs.getString("name"));
                c.setAbstract(rs.getInt("is_abstract") == 1);
                c.setInterface(rs.getInt("is_interface") == 1);
                c.setPositionX(rs.getDouble("position_x"));
                c.setPositionY(rs.getDouble("position_y"));
                loadAttributes(conn, c);
                loadMethods(conn, c);
                d.addUMLClass(c);
            }
        }
    }

    private void loadAttributes(Connection conn, UMLClass c) throws SQLException {
        String sql = "SELECT * FROM attributes WHERE class_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, c.getClassId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Attribute a = new Attribute();
                a.setAttributeId(rs.getInt("attr_id"));
                a.setName(rs.getString("name"));
                a.setType(rs.getString("type"));
                try { a.setVisibility(Visibility.valueOf(rs.getString("visibility"))); } catch (Exception ignored) {}
                a.setStatic(rs.getInt("is_static") == 1);
                c.addAttribute(a);
            }
        }
    }

    private void loadMethods(Connection conn, UMLClass c) throws SQLException {
        String sql = "SELECT * FROM methods WHERE class_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, c.getClassId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Method m = new Method();
                m.setMethodId(rs.getInt("method_id"));
                m.setName(rs.getString("name"));
                m.setReturnType(rs.getString("return_type"));
                String params = rs.getString("parameters");
                if (params != null && !params.isBlank()) {
                    m.setParameters(List.of(params.split(",")));
                }
                try { m.setVisibility(Visibility.valueOf(rs.getString("visibility"))); } catch (Exception ignored) {}
                m.setAbstract(rs.getInt("is_abstract") == 1);
                c.addMethod(m);
            }
        }
    }

    private void loadRelationships(Connection conn, UMLDiagram d) throws SQLException {
        String sql = "SELECT * FROM relationships WHERE diagram_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, d.getDiagramId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int srcId = rs.getInt("source_class_id");
                int tgtId = rs.getInt("target_class_id");
                UMLClass src = d.getClasses().stream().filter(c -> c.getClassId() == srcId).findFirst().orElse(null);
                UMLClass tgt = d.getClasses().stream().filter(c -> c.getClassId() == tgtId).findFirst().orElse(null);
                if (src == null || tgt == null) continue;

                String type = rs.getString("rel_type");
                Relationship r;
                switch (type) {
                    case "INHERITANCE", "REALIZATION" -> {
                        InheritanceRelationship ir = new InheritanceRelationship(rs.getInt("rel_id"), src, tgt, rs.getInt("is_interface_rel") == 1);
                        r = ir;
                    }
                    case "DEPENDENCY" -> {
                        r = new DependencyRelationship(rs.getInt("rel_id"), src, tgt, rs.getString("dependency_type"));
                    }
                    default -> {
                        AssociationRelationship ar = new AssociationRelationship(
                                rs.getInt("rel_id"), src, tgt,
                                rs.getInt("is_composition") == 1,
                                rs.getInt("is_aggregation") == 1,
                                Navigability.UNIDIRECTIONAL);
                        r = ar;
                    }
                }
                r.setSourceMultiplicity(rs.getString("source_multiplicity"));
                r.setTargetMultiplicity(rs.getString("target_multiplicity"));
                r.setLabel(rs.getString("label"));
                d.addRelationship(r);
            }
        }
    }

    private UMLDiagram mapDiagramRow(ResultSet rs) throws SQLException {
        UMLDiagram d = new UMLDiagram();
        d.setDiagramId(rs.getInt("diagram_id"));
        d.setProjectId(rs.getInt("project_id"));
        d.setTitle(rs.getString("title"));
        try { d.setSourceType(SourceType.valueOf(rs.getString("source_type"))); } catch (Exception ignored) {}
        try {
            d.setCreatedDate(SDF.parse(rs.getString("created_date")));
            d.setLastModifiedDate(SDF.parse(rs.getString("last_modified")));
        } catch (Exception ignored) {}
        return d;
    }
}
