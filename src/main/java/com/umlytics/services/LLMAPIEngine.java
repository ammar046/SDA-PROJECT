package com.umlytics.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.umlytics.domain.EvaluationReport;
import com.umlytics.domain.ProjectContext;
import com.umlytics.domain.UMLModel;
import com.umlytics.exceptions.AIEngineException;
import com.umlytics.exceptions.ParseResponseException;
import com.umlytics.interfaces.IAIEngine;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Properties;

// GRASP: Pure Fabrication
public class LLMAPIEngine implements IAIEngine {
    private static final Gson GSON = new Gson();

    private String apiKey;
    private String apiEndpoint;
    private String modelName;
    private HttpClient httpClient;

    public LLMAPIEngine() {
        loadConfig();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public UMLModel generateFromText(String desc) {
        if (desc == null || desc.isBlank()) {
            throw new AIEngineException("Description cannot be empty.", null);
        }
        UMLModel model = new UMLModel();
        model.setRawJson("{\"input\":\"" + desc.replace("\"", "\\\"") + "\"}");
        return model;
    }

    @Override
    public EvaluationReport evaluateDesign(UMLModel m) {
        EvaluationReport report = new EvaluationReport();
        report.setCouplingScore(6.0);
        report.setCohesionScore(6.5);
        report.setSolidScore(6.0);
        report.setSuggestions(new ArrayList<>());
        report.getSuggestions().add("Consider splitting responsibilities to improve SRP.");
        report.getSuggestions().add("Reduce direct dependencies between concrete classes.");
        return report;
    }

    @Override
    public String consultDesign(String q, ProjectContext ctx) {
        String context = ctx == null ? "" : ctx.buildContextPrompt();
        return "AI guidance for query: " + q + "\nContext: " + context;
    }

    @Override
    public String generateStructure(UMLModel m) {
        return "{\"skeletons\":{}}";
    }

    @Override
    public UMLModel analyzeImage(byte[] data) {
        if (data == null || data.length == 0) {
            throw new AIEngineException("Uploaded image data is empty.", null);
        }
        UMLModel model = new UMLModel();
        model.setRawJson("{\"analyzedImage\":true,\"bytes\":" + data.length + "}");
        return model;
    }

    private String buildPrompt(String input) {
        return "You are a UML class diagram expert. Input: " + input;
    }

    private UMLModel parseResponse(String json) {
        try {
            JsonObject object = GSON.fromJson(json, JsonObject.class);
            UMLModel model = new UMLModel();
            model.setRawJson(object.toString());
            return model;
        } catch (Exception e) {
            throw new ParseResponseException("Failed to parse AI response.", e);
        }
    }

    private void loadConfig() {
        Properties properties = new Properties();
        try (InputStream in = LLMAPIEngine.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (Exception e) {
            throw new AIEngineException("Failed to load AI config.", e);
        }
        apiKey = properties.getProperty("ai.api.key", "");
        apiEndpoint = properties.getProperty("ai.api.endpoint", "");
        modelName = properties.getProperty("ai.api.model", "gpt-4o");
    }
}
