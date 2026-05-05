package com.umlytics.domain;

import com.umlytics.enums.SourceType;
import com.umlytics.enums.RelationshipType;
import com.umlytics.enums.RenderState;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;

public class UMLDiagram {
    private UUID diagramId;
    private UUID projectId;
    private String title;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private SourceType sourceType;
    private RenderState renderState = RenderState.PENDING;
    private String defaultClassHeaderColor = "Blue";
    private String defaultClassBorderColor = "Blue";
    private double defaultClassFontSize = 12.0;
    private double defaultClassWidth = 200.0;
    private double defaultClassHeight = 140.0;
    private String defaultEdgeColor = "Purple";
    private boolean defaultEdgeDashed;
    private RelationshipType defaultRelationshipType = RelationshipType.ASSOCIATION;
    private final List<ConceptualClass> classes = new ArrayList<>();
    private final List<Relationship> relationships = new ArrayList<>();
    /** UI-only messages (e.g. code import skips); not persisted to DB. */
    private List<String> importNotes;

    public void render() {
        // UI rendering is handled by the JavaFX UI layer.
    }

    public boolean validate() {
        return classes != null && relationships != null;
    }

    public String serialize() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append("\"diagramId\":\"").append(diagramId == null ? "" : diagramId).append("\",");
        builder.append("\"projectId\":\"").append(projectId == null ? "" : projectId).append("\",");
        builder.append("\"title\":\"").append(title == null ? "" : escapeJson(title)).append("\",");
        builder.append("\"sourceType\":\"").append(sourceType == null ? "" : sourceType.name()).append("\",");
        builder.append("\"defaults\":{")
                .append("\"classHeaderColor\":\"").append(escapeJson(defaultClassHeaderColor)).append("\",")
                .append("\"classBorderColor\":\"").append(escapeJson(defaultClassBorderColor)).append("\",")
                .append("\"classFontSize\":").append(defaultClassFontSize).append(",")
                .append("\"classWidth\":").append(defaultClassWidth).append(",")
                .append("\"classHeight\":").append(defaultClassHeight).append(",")
                .append("\"edgeColor\":\"").append(escapeJson(defaultEdgeColor)).append("\",")
                .append("\"edgeDashed\":").append(defaultEdgeDashed).append(",")
                .append("\"relationshipType\":\"").append(defaultRelationshipType == null ? RelationshipType.ASSOCIATION.name() : defaultRelationshipType.name()).append("\"")
                .append("},");
        builder.append("\"classes\":[");
        for (int i = 0; i < classes.size(); i++) {
            ConceptualClass c = classes.get(i);
            builder.append("{")
                    .append("\"name\":\"").append(escapeJson(c.getName())).append("\",")
                    .append("\"x\":").append(c.getPositionX()).append(",")
                    .append("\"y\":").append(c.getPositionY()).append(",")
                    .append("\"headerColor\":\"").append(c.getHeaderColor() == null ? "Blue" : escapeJson(c.getHeaderColor())).append("\",")
                    .append("\"borderColor\":\"").append(c.getBorderColor() == null ? "Blue" : escapeJson(c.getBorderColor())).append("\",")
                    .append("\"memberFontSize\":").append(c.getMemberFontSize()).append(",")
                    .append("\"width\":").append(c.getClassWidth()).append(",")
                    .append("\"height\":").append(c.getClassHeight())
                    .append("}");
            if (i < classes.size() - 1) {
                builder.append(",");
            }
        }
        builder.append("],");
        builder.append("\"relationships\":[");
        for (int i = 0; i < relationships.size(); i++) {
            Relationship r = relationships.get(i);
            builder.append("{")
                    .append("\"type\":\"").append(r.getType().name()).append("\",")
                    .append("\"source\":\"").append(r.getSource() == null ? "" : escapeJson(r.getSource().getName())).append("\",")
                    .append("\"target\":\"").append(r.getTarget() == null ? "" : escapeJson(r.getTarget().getName())).append("\",")
                    .append("\"label\":\"").append(r.getLabel() == null ? "" : escapeJson(r.getLabel())).append("\",")
                    .append("\"sourceMultiplicity\":\"").append(r.getSourceMultiplicity() == null ? "" : escapeJson(r.getSourceMultiplicity())).append("\",")
                    .append("\"targetMultiplicity\":\"").append(r.getTargetMultiplicity() == null ? "" : escapeJson(r.getTargetMultiplicity())).append("\",")
                    .append("\"bendX\":").append(r.getBendX() == null ? "null" : r.getBendX()).append(",")
                    .append("\"edgeColor\":\"").append(r.getEdgeColor() == null ? "Black" : escapeJson(r.getEdgeColor())).append("\",")
                    .append("\"dashed\":").append(r.isDashed())
                    .append("}");
            if (i < relationships.size() - 1) {
                builder.append(",");
            }
        }
        builder.append("]");
        builder.append("}");
        return builder.toString();
    }

    public void addConceptualClass(ConceptualClass c) {
        classes.add(c);
        this.lastModifiedDate = LocalDateTime.now();
    }

    public void addUMLClass(ConceptualClass c) {
        addConceptualClass(c);
    }

    public void removeConceptualClass(UUID id) {
        classes.removeIf(c -> id.equals(c.getClassId()));
        this.lastModifiedDate = LocalDateTime.now();
    }

    public void removeUMLClass(UUID id) {
        removeConceptualClass(id);
    }

    public void addRelationship(Relationship r) {
        relationships.add(r);
        this.lastModifiedDate = LocalDateTime.now();
    }

    public void removeRelationship(UUID id) {
        relationships.removeIf(r -> id.equals(r.getRelationshipId()));
        this.lastModifiedDate = LocalDateTime.now();
    }

    public List<ConceptualClass> getClasses() {
        return classes;
    }

    public List<Relationship> getRelationships() {
        return relationships;
    }

    public UUID getDiagramId() {
        return diagramId;
    }

    public void setDiagramId(UUID diagramId) {
        this.diagramId = diagramId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public RenderState getRenderState() {
        return renderState;
    }

    public void setRenderState(RenderState renderState) {
        this.renderState = renderState;
    }

    public String getDefaultClassHeaderColor() {
        return defaultClassHeaderColor;
    }

    public void setDefaultClassHeaderColor(String defaultClassHeaderColor) {
        this.defaultClassHeaderColor = defaultClassHeaderColor == null || defaultClassHeaderColor.isBlank() ? "Blue" : defaultClassHeaderColor;
    }

    public String getDefaultClassBorderColor() {
        return defaultClassBorderColor;
    }

    public void setDefaultClassBorderColor(String defaultClassBorderColor) {
        this.defaultClassBorderColor = defaultClassBorderColor == null || defaultClassBorderColor.isBlank() ? "Blue" : defaultClassBorderColor;
    }

    public double getDefaultClassFontSize() {
        return defaultClassFontSize;
    }

    public void setDefaultClassFontSize(double defaultClassFontSize) {
        this.defaultClassFontSize = defaultClassFontSize <= 0 ? 12.0 : defaultClassFontSize;
    }

    public double getDefaultClassWidth() {
        return defaultClassWidth;
    }

    public void setDefaultClassWidth(double defaultClassWidth) {
        this.defaultClassWidth = defaultClassWidth <= 0 ? 200.0 : defaultClassWidth;
    }

    public double getDefaultClassHeight() {
        return defaultClassHeight;
    }

    public void setDefaultClassHeight(double defaultClassHeight) {
        this.defaultClassHeight = defaultClassHeight <= 0 ? 140.0 : defaultClassHeight;
    }

    public String getDefaultEdgeColor() {
        return defaultEdgeColor;
    }

    public void setDefaultEdgeColor(String defaultEdgeColor) {
        this.defaultEdgeColor = defaultEdgeColor == null || defaultEdgeColor.isBlank() ? "Black" : defaultEdgeColor;
    }

    public boolean isDefaultEdgeDashed() {
        return defaultEdgeDashed;
    }

    public void setDefaultEdgeDashed(boolean defaultEdgeDashed) {
        this.defaultEdgeDashed = defaultEdgeDashed;
    }

    public RelationshipType getDefaultRelationshipType() {
        return defaultRelationshipType;
    }

    public void setDefaultRelationshipType(RelationshipType defaultRelationshipType) {
        this.defaultRelationshipType = defaultRelationshipType == null ? RelationshipType.ASSOCIATION : defaultRelationshipType;
    }

    public List<String> getImportNotes() {
        return importNotes;
    }

    public void setImportNotes(List<String> importNotes) {
        this.importNotes = importNotes == null || importNotes.isEmpty() ? null : List.copyOf(importNotes);
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
