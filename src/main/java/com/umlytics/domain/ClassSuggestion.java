package com.umlytics.domain;

import com.umlytics.services.SkeletonResponseNormalizer;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ClassSuggestion {
    /**
     * Turns AI JSON {@code {"skeletons":{"ClassName":"java source..."}}} into plain Java text for copy-paste.
     * When {@code allowedClassNames} is non-empty, drops skeleton keys not in the diagram.
     */
    public static String combineSkeletonResponse(String response) {
        return combineSkeletonResponse(response, null);
    }

    public static String combineSkeletonResponse(String response, Set<String> allowedClassNames) {
        if (response == null) {
            return "";
        }
        String trimmed = response.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (!trimmed.startsWith("{")) {
            return trimmed;
        }
        try {
            String normalized = SkeletonResponseNormalizer.toCleanSkeletonsJson(trimmed);
            if (allowedClassNames != null && !allowedClassNames.isEmpty()) {
                normalized = SkeletonResponseNormalizer.filterSkeletonKeys(normalized, allowedClassNames);
            }
            Map<String, String> map = SkeletonResponseNormalizer.skeletonsToMap(normalized);
            if (map.isEmpty()) {
                return "// No valid Java skeletons parsed. Try Generate again or adjust the model.\n"
                        + "// Raw (truncated):\n// "
                        + trimmed.substring(0, Math.min(800, trimmed.length())).replace("\n", "\n// ");
            }
            StringBuilder combined = new StringBuilder();
            for (Map.Entry<String, String> e : map.entrySet()) {
                combined.append("// === ").append(e.getKey()).append(" ===\n");
                combined.append(e.getValue()).append("\n\n");
            }
            if (allowedClassNames != null && !allowedClassNames.isEmpty()) {
                for (String need : allowedClassNames) {
                    if (!map.containsKey(need)) {
                        combined.append("// === (missing) ").append(need).append(" ===\n");
                        combined.append("// The model did not return Java for this class.\n\n");
                    }
                }
            }
            return combined.toString().trim();
        } catch (Exception e) {
            return trimmed;
        }
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
        if (skeletonCode == null || skeletonCode.isBlank()) {
            return Map.of();
        }
        if (skeletonCode.trim().startsWith("{")) {
            try {
                Map<String, String> m = SkeletonResponseNormalizer.skeletonsToMap(
                        SkeletonResponseNormalizer.toCleanSkeletonsJson(skeletonCode.trim()));
                if (!m.isEmpty()) {
                    return m;
                }
            } catch (Exception ignored) {
                // fall through
            }
        }
        return Map.of("Generated", skeletonCode);
    }

    public void setCodeSkeletons(Map<String, String> skeletons) {
        if (skeletons == null || skeletons.isEmpty()) {
            this.skeletonCode = null;
            return;
        }
        this.skeletonCode = skeletons.values().iterator().next();
    }
}
