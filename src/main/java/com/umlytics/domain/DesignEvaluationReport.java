package com.umlytics.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class DesignEvaluationReport {
    private UUID reportId;
    private UUID diagramId;
    private UUID projectId;
    private float couplingScore;
    private float cohesionScore;
    private float solidScore;
    private String feedbackSummary;
    private LocalDateTime evaluationDate;

    public String getSummary() {
        return feedbackSummary == null || feedbackSummary.isBlank()
                ? "Coupling: " + couplingScore + ", Cohesion: " + cohesionScore + ", SOLID: " + solidScore
                : feedbackSummary;
    }

    public float getOverallScore() {
        return (couplingScore + cohesionScore + solidScore) / 3.0f;
    }

    public UUID getReportId() {
        return reportId;
    }

    public void setReportId(UUID reportId) {
        this.reportId = reportId;
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

    public float getCouplingScore() {
        return couplingScore;
    }

    public void setCouplingScore(float couplingScore) {
        this.couplingScore = couplingScore;
    }

    public float getCohesionScore() {
        return cohesionScore;
    }

    public void setCohesionScore(float cohesionScore) {
        this.cohesionScore = cohesionScore;
    }

    public float getSolidScore() {
        return solidScore;
    }

    public void setSolidScore(float solidScore) {
        this.solidScore = solidScore;
    }

    public String getFeedbackSummary() {
        return feedbackSummary;
    }

    public void setFeedbackSummary(String feedbackSummary) {
        this.feedbackSummary = feedbackSummary;
    }

    public LocalDateTime getEvaluationDate() {
        return evaluationDate;
    }

    public void setEvaluationDate(LocalDateTime evaluationDate) {
        this.evaluationDate = evaluationDate;
    }

    public List<String> getSuggestions() {
        if (feedbackSummary == null || feedbackSummary.isBlank()) {
            return List.of();
        }
        return List.of(feedbackSummary);
    }

    public void setSuggestions(List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            this.feedbackSummary = null;
            return;
        }
        this.feedbackSummary = String.join(" ", suggestions);
    }
}
