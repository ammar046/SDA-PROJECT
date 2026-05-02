package com.umlytics.domain;

import java.util.Date;
import java.util.Map;

public class StructureSuggestion {
    private int suggestionId;
    private int diagramId;
    private Map<String, String> codeSkeletons;
    private Date generatedDate;

    public Map<String, String> getSkeletons() {
        return codeSkeletons;
    }

    public void exportCode(String path) {
        // Export implementation will be added in service layer.
    }

    public int getSuggestionId() {
        return suggestionId;
    }

    public void setSuggestionId(int suggestionId) {
        this.suggestionId = suggestionId;
    }

    public int getDiagramId() {
        return diagramId;
    }

    public void setDiagramId(int diagramId) {
        this.diagramId = diagramId;
    }

    public void setCodeSkeletons(Map<String, String> codeSkeletons) {
        this.codeSkeletons = codeSkeletons;
    }

    public Date getGeneratedDate() {
        return generatedDate;
    }

    public void setGeneratedDate(Date generatedDate) {
        this.generatedDate = generatedDate;
    }
}
