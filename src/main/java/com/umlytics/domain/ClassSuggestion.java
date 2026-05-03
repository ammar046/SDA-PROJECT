package com.umlytics.domain;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Map;
import java.util.UUID;

public class ClassSuggestion {
    /**
     * Turns AI JSON {@code {"skeletons":{"ClassName":"java source..."}}} into plain Java text for copy-paste.
     * If the response is not that shape, returns the trimmed string unchanged.
     */
    public static String combineSkeletonResponse(String response) {
        if (response == null) {
            return "";
        }
        String trimmed = response.trim();
        if (trimmed.isEmpty() || !trimmed.startsWith("{")) {
            return trimmed;
        }
        try {
            JsonObject parsed = JsonParser.parseString(trimmed).getAsJsonObject();
            if (parsed.has("skeletons")) {
                JsonObject skeletons = parsed.getAsJsonObject("skeletons");
                StringBuilder combined = new StringBuilder();
                for (String key : skeletons.keySet()) {
                    if (!skeletons.get(key).isJsonPrimitive()) {
                        continue;
                    }
                    combined.append("// === ").append(key).append(" ===\n");
                    combined.append(skeletons.get(key).getAsString()).append("\n\n");
                }
                return combined.toString().trim();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return trimmed;
    }

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
