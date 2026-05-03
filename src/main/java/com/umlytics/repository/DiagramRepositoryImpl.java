package com.umlytics.repository;

import com.umlytics.db.DatabaseManager;
import com.umlytics.domain.AssociationRelationship;
import com.umlytics.domain.Attribute;
import com.umlytics.domain.ConceptualClass;
import com.umlytics.domain.DependencyRelationship;
import com.umlytics.domain.InheritanceRelationship;
import com.umlytics.domain.Method;
import com.umlytics.domain.Relationship;
import com.umlytics.domain.UMLDiagram;
import com.umlytics.enums.ClassType;
import com.umlytics.enums.RelationshipType;
import com.umlytics.enums.SourceType;
import com.umlytics.enums.Visibility;
import com.umlytics.exceptions.DatabaseException;
import com.umlytics.interfaces.IDiagramRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// GRASP: Pure Fabrication, Low Coupling
public class DiagramRepositoryImpl implements IDiagramRepository {
    public DiagramRepositoryImpl() {
    }

    @Override
    public void save(UMLDiagram d) {
        if (d.getDiagramId() == null) {
            d.setDiagramId(UUID.randomUUID());
        }
        if (d.getCreatedDate() == null) {
            d.setCreatedDate(LocalDateTime.now());
        }
        if (d.getLastModifiedDate() == null) {
            d.setLastModifiedDate(LocalDateTime.now());
        }
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                insertDiagramRow(conn, d);
                insertDiagramContent(conn, d);
                conn.commit();
            } catch (Exception inner) {
                conn.rollback();
                throw inner;
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to save diagram.", e);
        }
    }

    @Override
    public UMLDiagram findById(UUID id) {
        if (id == null) {
            return null;
        }
        String sql = "SELECT * FROM uml_diagram WHERE diagram_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            UMLDiagram diagram = null;
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    diagram = mapDiagramHeader(rs);
                }
            }
            if (diagram == null) {
                return null;
            }
            loadDiagramContent(conn, diagram);
            return diagram;
        } catch (Exception e) {
            throw new DatabaseException("Failed to find diagram by id.", e);
        }
    }

    @Override
    public List<UMLDiagram> findByProject(UUID projectId) {
        String sql = "SELECT diagram_id FROM uml_diagram WHERE project_id = ? ORDER BY last_updated DESC";
        List<UUID> ids = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projectId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(UUID.fromString(rs.getString("diagram_id")));
                }
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to list project diagrams.", e);
        }
        List<UMLDiagram> diagrams = new ArrayList<>();
        for (UUID id : ids) {
            UMLDiagram full = findById(id);
            if (full != null) {
                diagrams.add(full);
            }
        }
        return diagrams;
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
        if (d == null || d.getDiagramId() == null) {
            return;
        }
        String sql = "UPDATE uml_diagram SET project_id = ?, name = ?, source_type = ?, render_state = ?, last_updated = ? WHERE diagram_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, d.getProjectId() == null ? null : d.getProjectId().toString());
                ps.setString(2, d.getTitle() == null ? "Untitled Diagram" : d.getTitle());
                ps.setString(3, d.getSourceType() == null ? SourceType.MANUAL.name() : d.getSourceType().name());
                ps.setString(4, d.getRenderState() == null ? "PENDING" : d.getRenderState().name());
                ps.setString(5, LocalDateTime.now().toString());
                ps.setString(6, d.getDiagramId().toString());
                ps.executeUpdate();
            }
            deleteDiagramContent(conn, d.getDiagramId());
            insertDiagramContent(conn, d);
            conn.commit();
        } catch (Exception e) {
            throw new DatabaseException("Failed to update diagram.", e);
        }
    }

    private static void insertDiagramRow(Connection conn, UMLDiagram d) throws Exception {
        String sql = "INSERT INTO uml_diagram(diagram_id, project_id, name, source_type, render_state, last_updated) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, d.getDiagramId().toString());
            ps.setString(2, d.getProjectId() == null ? null : d.getProjectId().toString());
            ps.setString(3, d.getTitle() == null ? "Untitled Diagram" : d.getTitle());
            ps.setString(4, d.getSourceType() == null ? SourceType.MANUAL.name() : d.getSourceType().name());
            ps.setString(5, d.getRenderState() == null ? "PENDING" : d.getRenderState().name());
            ps.setString(6, LocalDateTime.now().toString());
            ps.executeUpdate();
        }
    }

    private static void deleteDiagramContent(Connection conn, UUID diagramId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM relationship WHERE diagram_id = ?")) {
            ps.setString(1, diagramId.toString());
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM attribute WHERE class_id IN (SELECT class_id FROM conceptual_class WHERE diagram_id = ?)")) {
            ps.setString(1, diagramId.toString());
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM method WHERE class_id IN (SELECT class_id FROM conceptual_class WHERE diagram_id = ?)")) {
            ps.setString(1, diagramId.toString());
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM conceptual_class WHERE diagram_id = ?")) {
            ps.setString(1, diagramId.toString());
            ps.executeUpdate();
        }
    }

    private static void insertDiagramContent(Connection conn, UMLDiagram d) throws Exception {
        UUID diagramId = d.getDiagramId();
        for (ConceptualClass c : d.getClasses()) {
            if (c.getClassId() == null) {
                c.setClassId(UUID.randomUUID());
            }
            c.setDiagramId(diagramId);
            insertClass(conn, c);
            for (Attribute a : c.getAttributes()) {
                if (a.getAttributeId() == null) {
                    a.setAttributeId(UUID.randomUUID());
                }
                insertAttribute(conn, c.getClassId(), a);
            }
            for (Method m : c.getMethods()) {
                if (m.getMethodId() == null) {
                    m.setMethodId(UUID.randomUUID());
                }
                insertMethod(conn, c.getClassId(), m);
            }
        }
        for (Relationship r : d.getRelationships()) {
            if (r.getRelationshipId() == null) {
                r.setRelationshipId(UUID.randomUUID());
            }
            if (r.getSource() == null || r.getTarget() == null) {
                continue;
            }
            if (r.getSource().getClassId() == null || r.getTarget().getClassId() == null) {
                continue;
            }
            insertRelationship(conn, diagramId, r);
        }
    }

    private static void insertClass(Connection conn, ConceptualClass c) throws Exception {
        String sql = "INSERT INTO conceptual_class(class_id, diagram_id, class_name, class_type, visibility, position_x, position_y, "
                + "header_color, border_color, member_font_size, class_width, class_height) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getClassId().toString());
            ps.setString(2, c.getDiagramId().toString());
            ps.setString(3, c.getName());
            ps.setString(4, c.getClassType() == null ? ClassType.ENTITY.name() : c.getClassType().name());
            ps.setString(5, c.getVisibility() == null ? Visibility.PUBLIC.name() : c.getVisibility().name());
            ps.setDouble(6, c.getPositionX());
            ps.setDouble(7, c.getPositionY());
            ps.setString(8, c.getHeaderColor());
            ps.setString(9, c.getBorderColor());
            ps.setDouble(10, c.getMemberFontSize());
            ps.setDouble(11, c.getClassWidth());
            ps.setDouble(12, c.getClassHeight());
            ps.executeUpdate();
        }
    }

    private static void insertAttribute(Connection conn, UUID classId, Attribute a) throws Exception {
        String sql = "INSERT INTO attribute(attribute_id, class_id, attribute_name, data_type, default_value, visibility) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, a.getAttributeId().toString());
            ps.setString(2, classId.toString());
            ps.setString(3, a.getAttributeName());
            ps.setString(4, a.getDataType());
            ps.setString(5, a.getDefaultValue());
            ps.setString(6, a.getVisibility() == null ? Visibility.PRIVATE.name() : a.getVisibility().name());
            ps.executeUpdate();
        }
    }

    private static void insertMethod(Connection conn, UUID classId, Method m) throws Exception {
        String sql = "INSERT INTO method(method_id, class_id, method_name, return_type, parameters, visibility) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getMethodId().toString());
            ps.setString(2, classId.toString());
            ps.setString(3, m.getMethodName());
            ps.setString(4, m.getReturnType());
            ps.setString(5, m.getParameters());
            ps.setString(6, m.getVisibility() == null ? Visibility.PUBLIC.name() : m.getVisibility().name());
            ps.executeUpdate();
        }
    }

    private static void insertRelationship(Connection conn, UUID diagramId, Relationship r) throws Exception {
        String sql = "INSERT INTO relationship(relationship_id, diagram_id, relationship_type, source_class_id, target_class_id, "
                + "source_multiplicity, target_multiplicity, label, bend_x, edge_color, dashed) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getRelationshipId().toString());
            ps.setString(2, diagramId.toString());
            ps.setString(3, r.getType().name());
            ps.setString(4, r.getSource().getClassId().toString());
            ps.setString(5, r.getTarget().getClassId().toString());
            ps.setString(6, r.getSourceMultiplicity());
            ps.setString(7, r.getTargetMultiplicity());
            ps.setString(8, r.getLabel());
            if (r.getBendX() == null) {
                ps.setObject(9, null);
            } else {
                ps.setDouble(9, r.getBendX());
            }
            ps.setString(10, r.getEdgeColor() == null ? "Black" : r.getEdgeColor());
            ps.setInt(11, r.isDashed() ? 1 : 0);
            ps.executeUpdate();
        }
    }

    private static UMLDiagram mapDiagramHeader(ResultSet rs) throws Exception {
        UMLDiagram diagram = new UMLDiagram();
        diagram.setDiagramId(UUID.fromString(rs.getString("diagram_id")));
        String projectId = rs.getString("project_id");
        if (projectId != null && !projectId.isBlank()) {
            diagram.setProjectId(UUID.fromString(projectId));
        }
        diagram.setTitle(rs.getString("name"));
        diagram.setSourceType(SourceType.valueOf(rs.getString("source_type")));
        String rsState = rs.getString("render_state");
        if (rsState != null && !rsState.isBlank()) {
            try {
                diagram.setRenderState(com.umlytics.enums.RenderState.valueOf(rsState));
            } catch (IllegalArgumentException ignored) {
                diagram.setRenderState(com.umlytics.enums.RenderState.PENDING);
            }
        }
        diagram.setLastModifiedDate(LocalDateTime.parse(rs.getString("last_updated")));
        diagram.setCreatedDate(diagram.getLastModifiedDate());
        return diagram;
    }

    private static void loadDiagramContent(Connection conn, UMLDiagram diagram) throws Exception {
        UUID diagramId = diagram.getDiagramId();
        Map<UUID, ConceptualClass> byId = new HashMap<>();
        String clsSql = "SELECT * FROM conceptual_class WHERE diagram_id = ? ORDER BY class_name";
        try (PreparedStatement ps = conn.prepareStatement(clsSql)) {
            ps.setString(1, diagramId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ConceptualClass c = mapClass(rs, diagramId);
                    byId.put(c.getClassId(), c);
                    diagram.getClasses().add(c);
                }
            }
        }
        for (ConceptualClass c : diagram.getClasses()) {
            loadAttributes(conn, c);
            loadMethods(conn, c);
        }
        String relSql = "SELECT * FROM relationship WHERE diagram_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(relSql)) {
            ps.setString(1, diagramId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Relationship r = mapRelationship(rs, byId);
                    if (r != null) {
                        diagram.getRelationships().add(r);
                    }
                }
            }
        }
    }

    private static ConceptualClass mapClass(ResultSet rs, UUID diagramId) throws Exception {
        ConceptualClass c = new ConceptualClass();
        c.setClassId(UUID.fromString(rs.getString("class_id")));
        c.setDiagramId(diagramId);
        c.setName(rs.getString("class_name"));
        String ct = rs.getString("class_type");
        if (ct != null && !ct.isBlank()) {
            c.setClassType(ClassType.valueOf(ct));
        }
        String vis = rs.getString("visibility");
        if (vis != null && !vis.isBlank()) {
            c.setVisibility(Visibility.valueOf(vis));
        }
        c.setPositionX(rs.getDouble("position_x"));
        c.setPositionY(rs.getDouble("position_y"));
        c.setHeaderColor(rs.getString("header_color"));
        c.setBorderColor(rs.getString("border_color"));
        c.setMemberFontSize(rs.getDouble("member_font_size"));
        c.setClassWidth(rs.getDouble("class_width"));
        c.setClassHeight(rs.getDouble("class_height"));
        return c;
    }

    private static void loadAttributes(Connection conn, ConceptualClass c) throws Exception {
        String sql = "SELECT * FROM attribute WHERE class_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getClassId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Attribute a = new Attribute();
                    a.setAttributeId(UUID.fromString(rs.getString("attribute_id")));
                    a.setAttributeName(rs.getString("attribute_name"));
                    a.setDataType(rs.getString("data_type"));
                    a.setDefaultValue(rs.getString("default_value"));
                    String vis = rs.getString("visibility");
                    if (vis != null && !vis.isBlank()) {
                        a.setVisibility(Visibility.valueOf(vis));
                    }
                    c.addAttribute(a);
                }
            }
        }
    }

    private static void loadMethods(Connection conn, ConceptualClass c) throws Exception {
        String sql = "SELECT * FROM method WHERE class_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getClassId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Method m = new Method();
                    m.setMethodId(UUID.fromString(rs.getString("method_id")));
                    m.setMethodName(rs.getString("method_name"));
                    m.setReturnType(rs.getString("return_type"));
                    m.setParameters(rs.getString("parameters"));
                    String vis = rs.getString("visibility");
                    if (vis != null && !vis.isBlank()) {
                        m.setVisibility(Visibility.valueOf(vis));
                    }
                    c.addMethod(m);
                }
            }
        }
    }

    private static Relationship mapRelationship(ResultSet rs, Map<UUID, ConceptualClass> byId) throws Exception {
        UUID id = UUID.fromString(rs.getString("relationship_id"));
        String typeStr = rs.getString("relationship_type");
        if (typeStr == null || typeStr.isBlank()) {
            return null;
        }
        RelationshipType type = RelationshipType.valueOf(typeStr);
        UUID srcId = UUID.fromString(rs.getString("source_class_id"));
        UUID tgtId = UUID.fromString(rs.getString("target_class_id"));
        ConceptualClass src = byId.get(srcId);
        ConceptualClass tgt = byId.get(tgtId);
        if (src == null || tgt == null) {
            return null;
        }
        Relationship r;
        switch (type) {
            case COMPOSITION:
            case AGGREGATION:
            case ASSOCIATION: {
                AssociationRelationship a = new AssociationRelationship();
                a.setComposition(type == RelationshipType.COMPOSITION);
                a.setAggregation(type == RelationshipType.AGGREGATION);
                r = a;
                break;
            }
            case INHERITANCE:
            case REALIZATION: {
                InheritanceRelationship i = new InheritanceRelationship();
                i.setInterface(type == RelationshipType.REALIZATION);
                r = i;
                break;
            }
            case DEPENDENCY:
                r = new DependencyRelationship();
                break;
            default:
                r = new AssociationRelationship();
        }
        r.setRelationshipId(id);
        r.setSourceClass(src);
        r.setTargetClass(tgt);
        r.setSourceMultiplicity(rs.getString("source_multiplicity"));
        r.setTargetMultiplicity(rs.getString("target_multiplicity"));
        r.setLabel(rs.getString("label"));
        double bend = rs.getDouble("bend_x");
        if (rs.wasNull()) {
            r.setBendX(null);
        } else {
            r.setBendX(bend);
        }
        r.setEdgeColor(rs.getString("edge_color"));
        r.setDashed(rs.getInt("dashed") != 0);
        return r;
    }
}
