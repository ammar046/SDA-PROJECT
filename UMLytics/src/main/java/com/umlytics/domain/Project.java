package com.umlytics.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents a UMLytics project that groups diagrams, chat and evaluations.
 * GRASP: Information Expert
 */
public class Project {
    private int    projectId;
    private String name;
    private String description;
    private Date   createdDate;
    private Date   lastModifiedDate;

    // In-memory aggregation — loaded on demand from DB
    private final List<UMLDiagram>      diagrams          = new ArrayList<>();
    private final List<ChatMessage>     chatHistory       = new ArrayList<>();
    private final List<EvaluationReport> evaluationHistory = new ArrayList<>();

    public Project() {}

    public Project(String name, String description) {
        this.name        = name;
        this.description = description;
        this.createdDate = new Date();
        this.lastModifiedDate = new Date();
    }

    // ---- Business methods ----
    public void createProject(String name, String description) {
        this.name        = name;
        this.description = description;
        this.createdDate = new Date();
        this.lastModifiedDate = new Date();
    }

    public void updateProject(String name, String description) {
        this.name             = name;
        this.description      = description;
        this.lastModifiedDate = new Date();
    }

    public void addDiagram(UMLDiagram d) {
        if (d != null && diagrams.stream().noneMatch(x -> x.getDiagramId() == d.getDiagramId()))
            diagrams.add(d);
    }

    public void removeDiagram(int id) {
        diagrams.removeIf(d -> d.getDiagramId() == id);
    }

    // ---- Getters / Setters ----
    public int    getProjectId()           { return projectId; }
    public void   setProjectId(int id)     { this.projectId = id; }
    public String getName()                { return name; }
    public void   setName(String n)        { this.name = n; }
    public String getDescription()         { return description; }
    public void   setDescription(String d) { this.description = d; }
    public Date   getCreatedDate()         { return createdDate; }
    public void   setCreatedDate(Date d)   { this.createdDate = d; }
    public Date   getLastModifiedDate()    { return lastModifiedDate; }
    public void   setLastModifiedDate(Date d) { this.lastModifiedDate = d; }

    public List<UMLDiagram>       getDiagrams()          { return new ArrayList<>(diagrams); }
    public List<ChatMessage>      getChatHistory()        { return new ArrayList<>(chatHistory); }
    public List<EvaluationReport> getEvaluationHistory() { return new ArrayList<>(evaluationHistory); }

    public void setChatHistory(List<ChatMessage> msgs) {
        chatHistory.clear();
        if (msgs != null) chatHistory.addAll(msgs);
    }

    public void setDiagrams(List<UMLDiagram> ds) {
        diagrams.clear();
        if (ds != null) diagrams.addAll(ds);
    }

    @Override
    public String toString() { return name; }
}
