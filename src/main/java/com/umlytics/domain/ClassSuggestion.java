package com.umlytics.domain;

import java.util.Map;
import java.util.UUID;

public class ClassSuggestion {
    private UUID suggestionId;
    private UUID classId;
    private UUID diagramId;
    private String skeletonCode;
    private String explanation;
    private Boolean accepted = false;

    public ClassSuggestion() {
    }

    public UUID getSuggestionId() {
        return suggestionId;
    }

    public void setSuggestionId(UUID suggestionId) {
        this.suggestionId = suggestionId;
    }

    public UUID getClassId() {
        return classId;
    }

    public void setClassId(UUID classId) {
        this.classId = classId;
    }

    public UUID getDiagramId() {
        return diagramId;
    }

    public void setDiagramId(UUID diagramId) {
        this.diagramId = diagramId;
    }

    public String getSkeletonCode() {
        return skeletonCode;
    }

    public void setSkeletonCode(String skeletonCode) {
        this.skeletonCode = skeletonCode;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }

    public Map<String, String> getSkeletons() {
        return Map.of("Generated", skeletonCode == null ? "" : skeletonCode);
    }

    public void setCodeSkeletons(Map<String, String> skeletons) {
        if (skeletons == null || skeletons.isEmpty()) {
            this.skeletonCode = null;
            return;
        }
        this.skeletonCode = skeletons.values().iterator().next();
    }
}
