package com.umlytics.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EvaluationReport {
    private int reportId;
    private int diagramId;
    private int projectId;
    private double couplingScore;
    private double cohesionScore;
    private double solidScore;
    private List<String> suggestions = new ArrayList<>();
    private Date generatedDate;

    public String getSummary() {
        return "Coupling: " + couplingScore + ", Cohesion: " + cohesionScore + ", SOLID: " + solidScore;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public double getOverallScore() {
        return (couplingScore + cohesionScore + solidScore) / 3.0;
    }

    public int getReportId() {
        return reportId;
    }

    public void setReportId(int reportId) {
        this.reportId = reportId;
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

    public double getCouplingScore() {
        return couplingScore;
    }

    public void setCouplingScore(double couplingScore) {
        this.couplingScore = couplingScore;
    }

    public double getCohesionScore() {
        return cohesionScore;
    }

    public void setCohesionScore(double cohesionScore) {
        this.cohesionScore = cohesionScore;
    }

    public double getSolidScore() {
        return solidScore;
    }

    public void setSolidScore(double solidScore) {
        this.solidScore = solidScore;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public Date getGeneratedDate() {
        return generatedDate;
    }

    public void setGeneratedDate(Date generatedDate) {
        this.generatedDate = generatedDate;
    }
}
