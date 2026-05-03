package com.umlytics.domain;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;

public class Project {
    private UUID projectId;
    private String name;
    private String description;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private final List<UMLDiagram> diagrams = new ArrayList<>();
    private final List<ChatMessage> chatHistory = new ArrayList<>();
    private final List<DesignEvaluationReport> evaluationHistory = new ArrayList<>();

    public void createProject(String name, String desc) {
        this.name = name;
        this.description = desc;
        LocalDateTime now = LocalDateTime.now();
        this.createdDate = now;
        this.lastModifiedDate = now;
        if (this.projectId == null) {
            this.projectId = UUID.randomUUID();
        }
    }

    public void updateProject(String name, String desc) {
        this.name = name;
        this.description = desc;
        this.lastModifiedDate = LocalDateTime.now();
    }

    public List<UMLDiagram> getDiagrams() {
        return diagrams;
    }

    public List<ChatMessage> getChatHistory() {
        return chatHistory;
    }

    public List<DesignEvaluationReport> getEvaluationHistory() {
        return evaluationHistory;
    }

    public void addDiagram(UMLDiagram d) {
        diagrams.add(d);
        this.lastModifiedDate = LocalDateTime.now();
    }

    public void removeDiagram(UUID id) {
        diagrams.removeIf(d -> id.equals(d.getDiagramId()));
        this.lastModifiedDate = LocalDateTime.now();
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
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
}
