package com.umlytics.repository;

import com.umlytics.db.DatabaseManager;
import com.umlytics.domain.AssociationRelationship;
import com.umlytics.domain.Attribute;
import com.umlytics.domain.DependencyRelationship;
import com.umlytics.domain.InheritanceRelationship;
import com.umlytics.domain.Method;
import com.umlytics.domain.Relationship;
import com.umlytics.domain.UMLDiagram;
import com.umlytics.domain.UMLClass;
import com.umlytics.enums.Navigability;
import com.umlytics.enums.RelationshipType;
import com.umlytics.enums.SourceType;
import com.umlytics.enums.Visibility;
import com.umlytics.exceptions.DatabaseException;
import com.umlytics.interfaces.IDiagramRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// GRASP: Pure Fabrication, Low Coupling
public class DiagramRepositoryImpl implements IDiagramRepository {
    private final Connection connection;

    public DiagramRepositoryImpl() {
        this.connection = null;
    }

    @Override
    public void save(UMLDiagram d) {
        String sql = "INSERT INTO uml_diagrams(project_id, title, source_type, created_date, last_modified_date, default_class_header_color, default_class_border_color, default_class_font_size, default_class_width, default_class_height, default_edge_color, default_edge_dashed, default_relationship_type, serialized_model) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Date now = new Date();
        if (d.getCreatedDate() == null) {
            d.setCreatedDate(now);
        }
        if (d.getLastModifiedDate() == null) {
            d.setLastModifiedDate(now);
        }
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            conn.setAutoCommit(false);
            ps.setInt(1, d.getProjectId());
            ps.setString(2, d.getTitle() == null ? "Untitled Diagram" : d.getTitle());
            ps.setString(3, d.getSourceType() == null ? SourceType.MANUAL.name() : d.getSourceType().name());
            ps.setString(4, d.getCreatedDate().toInstant().toString());
            ps.setString(5, d.getLastModifiedDate().toInstant().toString());
            ps.setString(6, safeString(d.getDefaultClassHeaderColor(), "Blue"));
            ps.setString(7, safeString(d.getDefaultClassBorderColor(), "Blue"));
            ps.setDouble(8, d.getDefaultClassFontSize() <= 0 ? 12 : d.getDefaultClassFontSize());
            ps.setDouble(9, d.getDefaultClassWidth() <= 0 ? 200 : d.getDefaultClassWidth());
            ps.setDouble(10, d.getDefaultClassHeight() <= 0 ? 140 : d.getDefaultClassHeight());
            ps.setString(11, safeString(d.getDefaultEdgeColor(), "Black"));
            ps.setInt(12, d.isDefaultEdgeDashed() ? 1 : 0);
            ps.setString(13, d.getDefaultRelationshipType() == null ? RelationshipType.ASSOCIATION.name() : d.getDefaultRelationshipType().name());
            ps.setString(14, d.serialize());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    d.setDiagramId(rs.getInt(1));
                }
            }
            saveClassesAndRelationships(conn, d);
            conn.commit();
        } catch (Exception e) {
            throw new DatabaseException("Failed to save diagram.", e);
        }
    }

    @Override
    public UMLDiagram findById(int id) {
        String sql = "SELECT * FROM uml_diagrams WHERE diagram_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                UMLDiagram diagram = mapDiagram(rs);
                loadClassesAndRelationships(conn, diagram);
                return diagram;
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to find diagram by id.", e);
        }
    }

    @Override
    public List<UMLDiagram> findByProject(int pid) {
        String sql = "SELECT * FROM uml_diagrams WHERE project_id = ? ORDER BY last_modified_date DESC";
        List<UMLDiagram> diagrams = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UMLDiagram diagram = mapDiagram(rs);
                    loadClassesAndRelationships(conn, diagram);
                    diagrams.add(diagram);
                }
            }
            return diagrams;
        } catch (Exception e) {
            throw new DatabaseException("Failed to list project diagrams.", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM uml_diagrams WHERE diagram_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            ps.setInt(1, id);
            ps.executeUpdate();
            conn.commit();
        } catch (Exception e) {
            throw new DatabaseException("Failed to delete diagram.", e);
        }
    }

    @Override
    public void update(UMLDiagram d) {
        String sql = "UPDATE uml_diagrams SET title = ?, source_type = ?, last_modified_date = ?, default_class_header_color = ?, default_class_border_color = ?, default_class_font_size = ?, default_class_width = ?, default_class_height = ?, default_edge_color = ?, default_edge_dashed = ?, default_relationship_type = ?, serialized_model = ? WHERE diagram_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            ps.setString(1, d.getTitle() == null ? "Untitled Diagram" : d.getTitle());
            ps.setString(2, d.getSourceType() == null ? SourceType.MANUAL.name() : d.getSourceType().name());
            ps.setString(3, Instant.now().toString());
            ps.setString(4, safeString(d.getDefaultClassHeaderColor(), "Blue"));
            ps.setString(5, safeString(d.getDefaultClassBorderColor(), "Blue"));
            ps.setDouble(6, d.getDefaultClassFontSize() <= 0 ? 12 : d.getDefaultClassFontSize());
            ps.setDouble(7, d.getDefaultClassWidth() <= 0 ? 200 : d.getDefaultClassWidth());
            ps.setDouble(8, d.getDefaultClassHeight() <= 0 ? 140 : d.getDefaultClassHeight());
            ps.setString(9, safeString(d.getDefaultEdgeColor(), "Black"));
            ps.setInt(10, d.isDefaultEdgeDashed() ? 1 : 0);
            ps.setString(11, d.getDefaultRelationshipType() == null ? RelationshipType.ASSOCIATION.name() : d.getDefaultRelationshipType().name());
            ps.setString(12, d.serialize());
            ps.setInt(13, d.getDiagramId());
            ps.executeUpdate();
            clearDiagramChildren(conn, d.getDiagramId());
            saveClassesAndRelationships(conn, d);
            conn.commit();
        } catch (Exception e) {
            throw new DatabaseException("Failed to update diagram.", e);
        }
    }

    private UMLDiagram mapDiagram(ResultSet rs) throws Exception {
        UMLDiagram diagram = new UMLDiagram();
        diagram.setDiagramId(rs.getInt("diagram_id"));
        diagram.setProjectId(rs.getInt("project_id"));
        diagram.setTitle(rs.getString("title"));
        diagram.setSourceType(SourceType.valueOf(rs.getString("source_type")));
        diagram.setCreatedDate(Date.from(Instant.parse(rs.getString("created_date"))));
        diagram.setLastModifiedDate(Date.from(Instant.parse(rs.getString("last_modified_date"))));
        diagram.setDefaultClassHeaderColor(safeString(rs.getString("default_class_header_color"), "Blue"));
        diagram.setDefaultClassBorderColor(safeString(rs.getString("default_class_border_color"), "Blue"));
        diagram.setDefaultClassFontSize(rs.getDouble("default_class_font_size") <= 0 ? 12 : rs.getDouble("default_class_font_size"));
        diagram.setDefaultClassWidth(rs.getDouble("default_class_width") <= 0 ? 200 : rs.getDouble("default_class_width"));
        diagram.setDefaultClassHeight(rs.getDouble("default_class_height") <= 0 ? 140 : rs.getDouble("default_class_height"));
        diagram.setDefaultEdgeColor(safeString(rs.getString("default_edge_color"), "Black"));
        diagram.setDefaultEdgeDashed(rs.getInt("default_edge_dashed") == 1);
        String defaultType = safeString(rs.getString("default_relationship_type"), RelationshipType.ASSOCIATION.name());
        try {
            diagram.setDefaultRelationshipType(RelationshipType.valueOf(defaultType));
        } catch (Exception ignored) {
            diagram.setDefaultRelationshipType(RelationshipType.ASSOCIATION);
        }
        return diagram;
    }

    private void saveClassesAndRelationships(Connection conn, UMLDiagram diagram) throws Exception {
        String classSql = "INSERT INTO uml_classes(diagram_id, name, is_abstract, is_interface, position_x, position_y, header_color, border_color, member_font_size, class_width, class_height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String attrSql = "INSERT INTO attributes(class_id, name, type, visibility, is_static) VALUES (?, ?, ?, ?, ?)";
        String methodSql = "INSERT INTO methods(class_id, name, return_type, parameters, visibility, is_abstract) VALUES (?, ?, ?, ?, ?, ?)";
        String relSql = "INSERT INTO relationships(diagram_id, source_class_id, target_class_id, relationship_type, source_multiplicity, target_multiplicity, label, is_composition, is_aggregation, is_interface, navigability, dependency_type, bend_x, edge_color, dashed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Map<UMLClass, Integer> classIds = new HashMap<>();
        try (PreparedStatement classPs = conn.prepareStatement(classSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement attrPs = conn.prepareStatement(attrSql);
             PreparedStatement methodPs = conn.prepareStatement(methodSql)) {
            for (UMLClass umlClass : diagram.getClasses()) {
                classPs.setInt(1, diagram.getDiagramId());
                classPs.setString(2, umlClass.getName() == null ? "UnnamedClass" : umlClass.getName());
                classPs.setInt(3, umlClass.isAbstract() ? 1 : 0);
                classPs.setInt(4, umlClass.isInterface() ? 1 : 0);
                classPs.setDouble(5, umlClass.getPositionX());
                classPs.setDouble(6, umlClass.getPositionY());
                classPs.setString(7, umlClass.getHeaderColor() == null ? "Blue" : umlClass.getHeaderColor());
                classPs.setString(8, umlClass.getBorderColor() == null ? "Blue" : umlClass.getBorderColor());
                classPs.setDouble(9, umlClass.getMemberFontSize());
                classPs.setDouble(10, umlClass.getClassWidth() <= 0 ? 200 : umlClass.getClassWidth());
                classPs.setDouble(11, umlClass.getClassHeight() <= 0 ? 140 : umlClass.getClassHeight());
                classPs.executeUpdate();

                try (ResultSet rs = classPs.getGeneratedKeys()) {
                    if (rs.next()) {
                        int classId = rs.getInt(1);
                        umlClass.setClassId(classId);
                        classIds.put(umlClass, classId);
                    }
                }

                int classId = umlClass.getClassId();
                for (Attribute attribute : umlClass.getAttributes()) {
                    attrPs.setInt(1, classId);
                    attrPs.setString(2, attribute.getName() == null ? "field" : attribute.getName());
                    attrPs.setString(3, attribute.getType() == null ? "String" : attribute.getType());
                    attrPs.setString(4, attribute.getVisibility() == null ? Visibility.PACKAGE.name() : attribute.getVisibility().name());
                    attrPs.setInt(5, attribute.isStatic() ? 1 : 0);
                    attrPs.executeUpdate();
                }

                for (Method method : umlClass.getMethods()) {
                    methodPs.setInt(1, classId);
                    methodPs.setString(2, method.getName() == null ? "method" : method.getName());
                    methodPs.setString(3, method.getReturnType() == null ? "void" : method.getReturnType());
                    methodPs.setString(4, String.join(",", method.getParameters()));
                    methodPs.setString(5, method.getVisibility() == null ? Visibility.PACKAGE.name() : method.getVisibility().name());
                    methodPs.setInt(6, method.isAbstract() ? 1 : 0);
                    methodPs.executeUpdate();
                }
            }
        }

        try (PreparedStatement relPs = conn.prepareStatement(relSql, Statement.RETURN_GENERATED_KEYS)) {
            for (Relationship relationship : diagram.getRelationships()) {
                Integer sourceId = classIds.get(relationship.getSource());
                Integer targetId = classIds.get(relationship.getTarget());
                if (sourceId == null || targetId == null) {
                    continue;
                }
                relPs.setInt(1, diagram.getDiagramId());
                relPs.setInt(2, sourceId);
                relPs.setInt(3, targetId);
                relPs.setString(4, relationship.getType().name());
                relPs.setString(5, relationship.getSourceMultiplicity());
                relPs.setString(6, relationship.getTargetMultiplicity());
                relPs.setString(7, relationship.getLabel());
                relPs.setInt(8, relationship instanceof AssociationRelationship ar && ar.isComposition() ? 1 : 0);
                relPs.setInt(9, relationship instanceof AssociationRelationship ar && ar.isAggregation() ? 1 : 0);
                relPs.setInt(10, relationship instanceof InheritanceRelationship ir && ir.isInterface() ? 1 : 0);
                relPs.setString(11, relationship instanceof AssociationRelationship ar && ar.getNavigability() != null
                        ? ar.getNavigability().name()
                        : Navigability.BIDIRECTIONAL.name());
                relPs.setString(12, relationship instanceof DependencyRelationship dr ? dr.getDependencyType() : null);
                if (relationship.getBendX() == null) {
                    relPs.setObject(13, null);
                } else {
                    relPs.setDouble(13, relationship.getBendX());
                }
                relPs.setString(14, relationship.getEdgeColor() == null ? "Black" : relationship.getEdgeColor());
                relPs.setInt(15, relationship.isDashed() ? 1 : 0);
                relPs.executeUpdate();
                try (ResultSet rs = relPs.getGeneratedKeys()) {
                    if (rs.next()) {
                        relationship.setRelationshipId(rs.getInt(1));
                    }
                }
            }
        }
    }

    private void clearDiagramChildren(Connection conn, int diagramId) throws Exception {
        try (PreparedStatement deleteRelationships = conn.prepareStatement("DELETE FROM relationships WHERE diagram_id = ?");
             PreparedStatement deleteAttrs = conn.prepareStatement("DELETE FROM attributes WHERE class_id IN (SELECT class_id FROM uml_classes WHERE diagram_id = ?)");
             PreparedStatement deleteMethods = conn.prepareStatement("DELETE FROM methods WHERE class_id IN (SELECT class_id FROM uml_classes WHERE diagram_id = ?)");
             PreparedStatement deleteClasses = conn.prepareStatement("DELETE FROM uml_classes WHERE diagram_id = ?")) {
            deleteRelationships.setInt(1, diagramId);
            deleteRelationships.executeUpdate();
            deleteAttrs.setInt(1, diagramId);
            deleteAttrs.executeUpdate();
            deleteMethods.setInt(1, diagramId);
            deleteMethods.executeUpdate();
            deleteClasses.setInt(1, diagramId);
            deleteClasses.executeUpdate();
        }
    }

    private void loadClassesAndRelationships(Connection conn, UMLDiagram diagram) throws Exception {
        Map<Integer, UMLClass> idToClass = new HashMap<>();
        String classSql = "SELECT * FROM uml_classes WHERE diagram_id = ?";
        String attrSql = "SELECT * FROM attributes WHERE class_id = ?";
        String methodSql = "SELECT * FROM methods WHERE class_id = ?";
        String relSql = "SELECT * FROM relationships WHERE diagram_id = ?";

        try (PreparedStatement classPs = conn.prepareStatement(classSql)) {
            classPs.setInt(1, diagram.getDiagramId());
            try (ResultSet classRs = classPs.executeQuery()) {
                while (classRs.next()) {
                    UMLClass umlClass = new UMLClass();
                    int classId = classRs.getInt("class_id");
                    umlClass.setClassId(classId);
                    umlClass.setName(classRs.getString("name"));
                    umlClass.setAbstract(classRs.getInt("is_abstract") == 1);
                    umlClass.setInterface(classRs.getInt("is_interface") == 1);
                    umlClass.setPositionX(classRs.getDouble("position_x"));
                    umlClass.setPositionY(classRs.getDouble("position_y"));
                    umlClass.setHeaderColor(classRs.getString("header_color"));
                    umlClass.setBorderColor(classRs.getString("border_color"));
                    umlClass.setMemberFontSize(classRs.getDouble("member_font_size"));
                    umlClass.setClassWidth(classRs.getDouble("class_width"));
                    umlClass.setClassHeight(classRs.getDouble("class_height"));
                    if (umlClass.getClassWidth() <= 0) {
                        umlClass.setClassWidth(200);
                    }
                    if (umlClass.getClassHeight() <= 0) {
                        umlClass.setClassHeight(140);
                    }
                    idToClass.put(classId, umlClass);
                    diagram.addUMLClass(umlClass);

                    try (PreparedStatement attrPs = conn.prepareStatement(attrSql)) {
                        attrPs.setInt(1, classId);
                        try (ResultSet attrRs = attrPs.executeQuery()) {
                            while (attrRs.next()) {
                                Attribute attribute = new Attribute();
                                attribute.setAttributeId(attrRs.getInt("attribute_id"));
                                attribute.setName(attrRs.getString("name"));
                                attribute.setType(attrRs.getString("type"));
                                attribute.setVisibility(Visibility.valueOf(attrRs.getString("visibility")));
                                attribute.setStatic(attrRs.getInt("is_static") == 1);
                                umlClass.addAttribute(attribute);
                            }
                        }
                    }

                    try (PreparedStatement methodPs = conn.prepareStatement(methodSql)) {
                        methodPs.setInt(1, classId);
                        try (ResultSet methodRs = methodPs.executeQuery()) {
                            while (methodRs.next()) {
                                Method method = new Method();
                                method.setMethodId(methodRs.getInt("method_id"));
                                method.setName(methodRs.getString("name"));
                                method.setReturnType(methodRs.getString("return_type"));
                                String params = methodRs.getString("parameters");
                                if (params != null && !params.isBlank()) {
                                    method.setParameters(List.of(params.split(",")));
                                }
                                method.setVisibility(Visibility.valueOf(methodRs.getString("visibility")));
                                method.setAbstract(methodRs.getInt("is_abstract") == 1);
                                umlClass.addMethod(method);
                            }
                        }
                    }
                }
            }
        }

        try (PreparedStatement relPs = conn.prepareStatement(relSql)) {
            relPs.setInt(1, diagram.getDiagramId());
            try (ResultSet relRs = relPs.executeQuery()) {
                while (relRs.next()) {
                    int sourceId = relRs.getInt("source_class_id");
                    int targetId = relRs.getInt("target_class_id");
                    UMLClass source = idToClass.get(sourceId);
                    UMLClass target = idToClass.get(targetId);
                    if (source == null || target == null) {
                        continue;
                    }

                    String type = relRs.getString("relationship_type");
                    Relationship relationship = switch (type) {
                        case "INHERITANCE", "REALIZATION" -> {
                            InheritanceRelationship r = new InheritanceRelationship();
                            r.setInterface(relRs.getInt("is_interface") == 1 || "REALIZATION".equals(type));
                            yield r;
                        }
                        case "DEPENDENCY" -> {
                            DependencyRelationship r = new DependencyRelationship();
                            r.setDependencyType(relRs.getString("dependency_type"));
                            yield r;
                        }
                        default -> {
                            AssociationRelationship r = new AssociationRelationship();
                            r.setComposition(relRs.getInt("is_composition") == 1 || "COMPOSITION".equals(type));
                            r.setAggregation(relRs.getInt("is_aggregation") == 1 || "AGGREGATION".equals(type));
                            String nav = relRs.getString("navigability");
                            if (nav != null && !nav.isBlank()) {
                                r.setNavigability(Navigability.valueOf(nav));
                            }
                            yield r;
                        }
                    };
                    relationship.setRelationshipId(relRs.getInt("relationship_id"));
                    relationship.setSourceClass(source);
                    relationship.setTargetClass(target);
                    relationship.setSourceMultiplicity(relRs.getString("source_multiplicity"));
                    relationship.setTargetMultiplicity(relRs.getString("target_multiplicity"));
                    relationship.setLabel(relRs.getString("label"));
                    Object bend = relRs.getObject("bend_x");
                    if (bend != null) {
                        relationship.setBendX(relRs.getDouble("bend_x"));
                    }
                    relationship.setEdgeColor(relRs.getString("edge_color"));
                    relationship.setDashed(relRs.getInt("dashed") == 1);
                    diagram.addRelationship(relationship);
                }
            }
        }
    }

    private String safeString(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
