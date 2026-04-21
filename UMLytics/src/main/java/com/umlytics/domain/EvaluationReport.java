package com.umlytics.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Holds the AI-generated design evaluation results for a diagram.
 * GRASP: Information Expert
 */
public class EvaluationReport {
    private int          reportId;
    private int          diagramId;
    private int          projectId;
    private double       couplingScore;
    private double       cohesionScore;
    private double       solidScore;
    private List<String> suggestions = new ArrayList<>();
    private Date         generatedDate;

    public EvaluationReport() {}

    public EvaluationReport(int reportId, int diagramId, int projectId,
                            double coupling, double cohesion, double solid,
                            List<String> suggestions, Date generatedDate) {
        this.reportId      = reportId;
        this.diagramId     = diagramId;
        this.projectId     = projectId;
        this.couplingScore = coupling;
        this.cohesionScore = cohesion;
        this.solidScore    = solid;
        this.suggestions   = suggestions != null ? suggestions : new ArrayList<>();
        this.generatedDate = generatedDate;
    }

    /** Returns averaged overall score 0-100 */
    public double getOverallScore() {
        return (couplingScore + cohesionScore + solidScore) / 3.0;
    }

    public String getSummary() {
        return String.format("Overall: %.1f | Coupling: %.1f | Cohesion: %.1f | SOLID: %.1f",
                getOverallScore(), couplingScore, cohesionScore, solidScore);
    }

    public List<String> getSuggestions() { return new ArrayList<>(suggestions); }
    public void setSuggestions(List<String> s) { this.suggestions = s != null ? s : new ArrayList<>(); }

    public int    getReportId()        { return reportId; }
    public void   setReportId(int id)  { this.reportId = id; }
    public int    getDiagramId()       { return diagramId; }
    public void   setDiagramId(int id) { this.diagramId = id; }
    public int    getProjectId()       { return projectId; }
    public void   setProjectId(int id) { this.projectId = id; }
    public double getCouplingScore()   { return couplingScore; }
    public void   setCouplingScore(double v) { this.couplingScore = v; }
    public double getCohesionScore()   { return cohesionScore; }
    public void   setCohesionScore(double v) { this.cohesionScore = v; }
    public double getSolidScore()      { return solidScore; }
    public void   setSolidScore(double v) { this.solidScore = v; }
    public Date   getGeneratedDate()   { return generatedDate; }
    public void   setGeneratedDate(Date d) { this.generatedDate = d; }
}
