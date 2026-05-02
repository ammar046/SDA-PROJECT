package com.umlytics.domain;

import com.umlytics.enums.SourceType;

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
        builder.append("\"classes\":[");
        for (int i = 0; i < classes.size(); i++) {
            UMLClass c = classes.get(i);
            builder.append("{")
                    .append("\"name\":\"").append(escapeJson(c.getName())).append("\",")
                    .append("\"x\":").append(c.getPositionX()).append(",")
                    .append("\"y\":").append(c.getPositionY()).append(",")
                    .append("\"headerColor\":\"").append(c.getHeaderColor() == null ? "Blue" : escapeJson(c.getHeaderColor())).append("\",")
                    .append("\"borderColor\":\"").append(c.getBorderColor() == null ? "Blue" : escapeJson(c.getBorderColor())).append("\",")
                    .append("\"memberFontSize\":").append(c.getMemberFontSize())
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

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
