package com.umlytics.domain.valueobjects;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds AI-generated code skeleton suggestions for diagram classes.
 * GRASP: Pure Fabrication
 */
public class StructureSuggestion {
    private int                 suggestionId;
    private int                 diagramId;
    private Map<String,String>  codeSkeletons = new HashMap<>();
    private Date                generatedDate;

    public StructureSuggestion() {}

    public StructureSuggestion(int id, int diagramId, Map<String,String> skeletons, Date date) {
        this.suggestionId  = id;
        this.diagramId     = diagramId;
        this.codeSkeletons = skeletons != null ? skeletons : new HashMap<>();
        this.generatedDate = date;
    }

    public Map<String,String> getSkeletons() {
        return new HashMap<>(codeSkeletons);
    }

    public void exportCode(String path) {
        try {
            StringBuilder sb = new StringBuilder();
            for (var entry : codeSkeletons.entrySet()) {
                sb.append("// === ").append(entry.getKey()).append(".java ===\n");
                sb.append(entry.getValue()).append("\n\n");
            }
            Files.writeString(Paths.get(path), sb.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to export code: " + e.getMessage(), e);
        }
    }

    public int    getSuggestionId()       { return suggestionId; }
    public void   setSuggestionId(int id) { this.suggestionId = id; }
    public int    getDiagramId()          { return diagramId; }
    public void   setDiagramId(int id)    { this.diagramId = id; }
    public void   setCodeSkeletons(Map<String,String> s) { this.codeSkeletons = s; }
    public Date   getGeneratedDate()      { return generatedDate; }
    public void   setGeneratedDate(Date d){ this.generatedDate = d; }
}
