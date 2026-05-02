package com.umlytics.domain;

import com.umlytics.enums.SourceType;
import com.umlytics.enums.RelationshipType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UMLDiagram {
    private int diagramId;
    private int projectId;
    private String title;
    private Date createdDate;
    private Date lastModifiedDate;
    private SourceType sourceType;
    private String defaultClassHeaderColor = "Blue";
    private String defaultClassBorderColor = "Blue";
    private double defaultClassFontSize = 12.0;
    private double defaultClassWidth = 200.0;
    private double defaultClassHeight = 140.0;
    private String defaultEdgeColor = "Black";
    private boolean defaultEdgeDashed;
    private RelationshipType defaultRelationshipType = RelationshipType.ASSOCIATION;
    private final List<UMLClass> classes = new ArrayList<>();
    private final List<Relationship> relationships = new ArrayList<>();

    public void render() {
        // UI rendering is handled by the JavaFX UI layer.
    }

    public boolean validate() {
        return classes != null && relationships != null;
    }

    public String serialize() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append("\"diagramId\":").append(diagramId).append(",");
        builder.append("\"projectId\":").append(projectId).append(",");
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
            UMLClass c = classes.get(i);
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

    public void addUMLClass(UMLClass c) {
        classes.add(c);
        this.lastModifiedDate = new Date();
    }

    public void removeUMLClass(int id) {
        classes.removeIf(c -> c.getClassId() == id);
        this.lastModifiedDate = new Date();
    }

    public void addRelationship(Relationship r) {
        relationships.add(r);
        this.lastModifiedDate = new Date();
    }

    public void removeRelationship(int id) {
        relationships.removeIf(r -> r.getRelationshipId() == id);
        this.lastModifiedDate = new Date();
    }

    public List<UMLClass> getClasses() {
        return classes;
    }

    public List<Relationship> getRelationships() {
        return relationships;
    }

    public int getDiagramId() {
        return diagramId;
    }

    public void setDiagramId(int diagramId) {
        this.diagramId = diagramId;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
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

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
