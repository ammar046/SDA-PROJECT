package com.umlytics.domain;

import com.umlytics.enums.SourceType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents a single UML class diagram within a project.
 * GRASP: Creator (owns UMLClass + Relationship), Information Expert
 */
public class UMLDiagram {
    private int          diagramId;
    private String       title;
    private Date         createdDate;
    private Date         lastModifiedDate;
    private SourceType   sourceType;
    private int          projectId;

    private final List<UMLClass>     classes       = new ArrayList<>();
    private final List<com.umlytics.domain.relationships.Relationship> relationships = new ArrayList<>();

    public UMLDiagram() {}

    public UMLDiagram(int id, String title, SourceType sourceType, int projectId) {
        this.diagramId   = id;
        this.title       = title;
        this.sourceType  = sourceType;
        this.projectId   = projectId;
        this.createdDate = new Date();
        this.lastModifiedDate = new Date();
    }

    // ---- Class management ----
    public void addUMLClass(UMLClass c) {
        if (c != null) {
            if (c.getClassId() == 0) {
                c.setClassId(classes.stream().mapToInt(UMLClass::getClassId).max().orElse(0) + 1);
            }
            classes.add(c);
        }
    }

    public void removeUMLClass(int id) {
        classes.removeIf(c -> c.getClassId() == id);
    }

    public List<UMLClass> getClasses() {
        return new ArrayList<>(classes);
    }

    // ---- Relationship management ----
    public void addRelationship(com.umlytics.domain.relationships.Relationship r) {
        if (r != null) {
            if (r.getRelationshipId() == 0) {
                r.setRelationshipId(relationships.stream().mapToInt(com.umlytics.domain.relationships.Relationship::getRelationshipId).max().orElse(0) + 1);
            }
            relationships.add(r);
        }
    }

    public void removeRelationship(int id) {
        relationships.removeIf(r -> r.getRelationshipId() == id);
    }

    public List<com.umlytics.domain.relationships.Relationship> getRelationships() {
        return new ArrayList<>(relationships);
    }

    /** Validates structural integrity (no orphan relationships) */
    public boolean validate() {
        for (var rel : relationships) {
            boolean sourceExists = classes.stream().anyMatch(c -> c.getClassId() == rel.getSourceClass().getClassId());
            boolean targetExists = classes.stream().anyMatch(c -> c.getClassId() == rel.getTargetClass().getClassId());
            if (!sourceExists || !targetExists) return false;
        }
        return true;
    }

    /** Serializes this diagram to a compact JSON string (via Gson) */
    public String serialize() {
        com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

    public void render() { /* Delegates to UI layer */ }

    // ---- Getters / Setters ----
    public int        getDiagramId()         { return diagramId; }
    public void       setDiagramId(int id)   { this.diagramId = id; }
    public String     getTitle()             { return title; }
    public void       setTitle(String t)     { this.title = t; }
    public Date       getCreatedDate()       { return createdDate; }
    public void       setCreatedDate(Date d) { this.createdDate = d; }
    public Date       getLastModifiedDate()  { return lastModifiedDate; }
    public void       setLastModifiedDate(Date d) { this.lastModifiedDate = d; }
    public SourceType getSourceType()        { return sourceType; }
    public void       setSourceType(SourceType t) { this.sourceType = t; }
    public int        getProjectId()         { return projectId; }
    public void       setProjectId(int pid)  { this.projectId = pid; }

    @Override
    public String toString() { return title; }
}
