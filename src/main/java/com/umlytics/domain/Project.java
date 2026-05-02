package com.umlytics.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Project {
    private int projectId;
    private String name;
    private String description;
    private Date createdDate;
    private Date lastModifiedDate;
    private final List<UMLDiagram> diagrams = new ArrayList<>();
    private final List<ChatMessage> chatHistory = new ArrayList<>();
    private final List<EvaluationReport> evaluationHistory = new ArrayList<>();

    public void createProject(String name, String desc) {
        this.name = name;
        this.description = desc;
        Date now = new Date();
        this.createdDate = now;
        this.lastModifiedDate = now;
    }

    public void updateProject(String name, String desc) {
        this.name = name;
        this.description = desc;
        this.lastModifiedDate = new Date();
    }

    public List<UMLDiagram> getDiagrams() {
        return diagrams;
    }

    public List<ChatMessage> getChatHistory() {
        return chatHistory;
    }

    public List<EvaluationReport> getEvaluationHistory() {
        return evaluationHistory;
    }

    public void addDiagram(UMLDiagram d) {
        diagrams.add(d);
        this.lastModifiedDate = new Date();
    }

    public void removeDiagram(int id) {
        diagrams.removeIf(d -> d.getDiagramId() == id);
        this.lastModifiedDate = new Date();
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
}
