package com.umlytics.service.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.umlytics.domain.Attribute;
import com.umlytics.domain.EvaluationReport;
import com.umlytics.domain.Method;
import com.umlytics.domain.UMLClass;
import com.umlytics.domain.relationships.AssociationRelationship;
import com.umlytics.domain.relationships.DependencyRelationship;
import com.umlytics.domain.relationships.InheritanceRelationship;
import com.umlytics.domain.relationships.Relationship;
import com.umlytics.domain.valueobjects.ProjectContext;
import com.umlytics.domain.valueobjects.UMLModel;
import com.umlytics.enums.Navigability;
import com.umlytics.enums.RelationshipType;
import com.umlytics.enums.Visibility;
import com.umlytics.interfaces.IAIEngine;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * Google Gemini 1.5 Flash API implementation for AI Engine.
 * GRASP: Pure Fabrication, Low Coupling
 */
public class LLMAPIEngine implements IAIEngine {

    private final String apiKey;
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";
    private final Gson gson = new Gson();

    public LLMAPIEngine() {
        // Section 9: Check env first, then system property (set by Main.java from file/dialog)
        String key = System.getenv("GEMINI_API_KEY");
        if (key == null || key.isBlank()) key = System.getProperty("GEMINI_API_KEY", "");
        this.apiKey = key;
    }

    // =====================================================================
    // IAIEngine implementation
    // =====================================================================

    @Override
    public UMLModel generateFromText(String description) {
        String prompt =
            "You are a software architect. Extract a high-level UML class diagram from the following description.\n" +
            "Output ONLY strictly valid JSON, no markdown, no explanation. Structure:\n" +
            "{ \"classes\": [{\"name\":\"ClassName\",\"fields\":[\"visibility name:Type\"]," +
            "\"methods\":[\"visibility name(params):ReturnType\"],\"isAbstract\":false,\"isInterface\":false}]," +
            "\"relationships\": [{\"source\":\"ClassName\",\"target\":\"ClassName\"," +
            "\"type\":\"INHERITANCE|REALIZATION|ASSOCIATION|DEPENDENCY|COMPOSITION|AGGREGATION\"}] }\n\n" +
            "Description:\n" + description;

        try {
            String response = callGemini(prompt);
            return parseResponseToModel(response);
        } catch (IOException e) {
            System.err.println("[LLMAPIEngine] Network error: " + e.getMessage());
            return new UMLModel(new ArrayList<>(), new ArrayList<>(), "{}");
        } catch (Exception e) {
            System.err.println("[LLMAPIEngine] Unexpected error in generateFromText: " + e.getMessage());
            return new UMLModel(new ArrayList<>(), new ArrayList<>(), "{}");
        }
    }

    @Override
    public EvaluationReport evaluateDesign(UMLModel model) {
        String json = gson.toJson(model);
        String prompt =
            "You are a senior software architect. Evaluate this UML diagram for coupling, cohesion, and SOLID compliance.\n" +
            "Output ONLY valid JSON:\n" +
            "{ \"coupling\": 85.0, \"cohesion\": 90.0, \"solid\": 80.0," +
            "\"suggestions\": [\"suggestion 1\", \"suggestion 2\"] }\n\n" + json;

        try {
            String response = callGemini(prompt);
            response = extractJsonBlock(response);
            JsonObject obj = gson.fromJson(response, JsonObject.class);
            EvaluationReport rep = new EvaluationReport();
            rep.setCouplingScore(obj.has("coupling") ? obj.get("coupling").getAsDouble() : 50.0);
            rep.setCohesionScore(obj.has("cohesion") ? obj.get("cohesion").getAsDouble() : 50.0);
            rep.setSolidScore(obj.has("solid")       ? obj.get("solid").getAsDouble()    : 50.0);
            List<String> suggs = new ArrayList<>();
            if (obj.has("suggestions")) {
                obj.getAsJsonArray("suggestions").forEach(s -> suggs.add(s.getAsString()));
            }
            rep.setSuggestions(suggs);
            rep.setGeneratedDate(new Date());
            return rep;
        } catch (IOException e) {
            System.err.println("[LLMAPIEngine] Network error in evaluateDesign: " + e.getMessage());
            return new EvaluationReport();
        } catch (Exception e) {
            System.err.println("[LLMAPIEngine] Bad JSON in evaluateDesign: " + e.getMessage());
            return new EvaluationReport();
        }
    }

    @Override
    public String consultDesign(String query, ProjectContext context) {
        String prompt =
            "You are UMLytics AI Assistant, a UML design intelligence expert.\nContext:\n" +
            context.buildContextPrompt() + "\n\nUser Question:\n" + query;
        try {
            return callGemini(prompt);
        } catch (IOException e) {
            System.err.println("[LLMAPIEngine] Network error in consultDesign: " + e.getMessage());
            return "Network error — please check your connection.";
        } catch (Exception e) {
            System.err.println("[LLMAPIEngine] Error in consultDesign: " + e.getMessage());
            return "Error processing your request.";
        }
    }

    @Override
    public String generateStructure(UMLModel model) {
        String prompt =
            "Generate complete Java class skeletons for this UML model.\n" +
            "Follow GRASP and GoF patterns where applicable.\n" +
            "Separate each class with a comment marker: // === ClassName ===\n" +
            "Return ONLY valid Java code, no markdown.\n\n" +
            gson.toJson(model);
        try {
            return callGemini(prompt);
        } catch (IOException e) {
            System.err.println("[LLMAPIEngine] Network error in generateStructure: " + e.getMessage());
            return "// Network error — code generation failed.";
        } catch (Exception e) {
            System.err.println("[LLMAPIEngine] Error in generateStructure: " + e.getMessage());
            return "// Unexpected error during code generation.";
        }
    }

    @Override
    public UMLModel analyzeImage(byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            return new UMLModel(new ArrayList<>(), new ArrayList<>(), "{}");
        }

        String base64 = Base64.getEncoder().encodeToString(imageData);
        String prompt =
            "Analyse this UML class diagram image. Extract all classes, their attributes, methods, and relationships.\n" +
            "Return ONLY valid JSON:\n" +
            "{ \"classes\": [{\"name\":\"\",\"fields\":[\"name:Type\"],\"methods\":[\"name():ReturnType\"]}]," +
            "\"relationships\": [{\"source\":\"\",\"target\":\"\"," +
            "\"type\":\"INHERITANCE|ASSOCIATION|COMPOSITION|AGGREGATION|DEPENDENCY\"}] }";

        try {
            String response = callGeminiWithImage(prompt, base64);
            return parseResponseToModel(response);
        } catch (IOException e) {
            System.err.println("[LLMAPIEngine] Network error in analyzeImage: " + e.getMessage());
            return new UMLModel(new ArrayList<>(), new ArrayList<>(), "{}");
        } catch (Exception e) {
            System.err.println("[LLMAPIEngine] Error in analyzeImage: " + e.getMessage());
            return new UMLModel(new ArrayList<>(), new ArrayList<>(), "{}");
        }
    }

    // =====================================================================
    // HTTP helpers
    // =====================================================================

    private String callGemini(String prompt) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("[LLMAPIEngine] API key not set.");
            return "{}";
        }

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(API_URL + apiKey);
            request.setHeader("Content-Type", "application/json");

            JsonObject part = new JsonObject();
            part.addProperty("text", prompt);

            JsonObject contents = buildContents(new JsonObject[]{part});
            request.setEntity(new StringEntity(gson.toJson(contents), ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = client.execute(request)) {
                String body;
                try { body = EntityUtils.toString(response.getEntity()); }
                catch (org.apache.hc.core5.http.ParseException pe) { body = "{}"; }
                return extractTextFromResponse(body);
            }
        }
    }

    private String callGeminiWithImage(String prompt, String base64Image) throws IOException {
        if (apiKey == null || apiKey.isBlank()) return "{}";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(API_URL + apiKey);
            request.setHeader("Content-Type", "application/json");

            // Text part
            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", prompt);

            // Image part (inline data)
            JsonObject inlineData = new JsonObject();
            inlineData.addProperty("mime_type", "image/png");
            inlineData.addProperty("data", base64Image);
            JsonObject imagePart = new JsonObject();
            imagePart.add("inline_data", inlineData);

            JsonObject contents = buildContents(new JsonObject[]{textPart, imagePart});
            request.setEntity(new StringEntity(gson.toJson(contents), ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = client.execute(request)) {
                String body;
                try { body = EntityUtils.toString(response.getEntity()); }
                catch (org.apache.hc.core5.http.ParseException pe) { body = "{}"; }
                return extractTextFromResponse(body);
            }
        }
    }

    private JsonObject buildContents(JsonObject[] parts) {
        JsonObject msg = new JsonObject();
        msg.add("parts", gson.toJsonTree(parts));
        JsonObject payload = new JsonObject();
        payload.add("contents", gson.toJsonTree(new JsonObject[]{msg}));
        return payload;
    }

    private String extractTextFromResponse(String responseBody) {
        try {
            JsonObject res = gson.fromJson(responseBody, JsonObject.class);
            return res.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        } catch (Exception e) {
            System.err.println("[LLMAPIEngine] Failed to parse API response. Body: "
                    + responseBody.substring(0, Math.min(200, responseBody.length())));
            return "{}";
        }
    }

    // =====================================================================
    // JSON parsing
    // =====================================================================

    private String extractJsonBlock(String text) {
        if (text == null) return "{}";
        // Strip markdown code fences
        text = text.replaceAll("```json", "").replaceAll("```", "").trim();
        int start = text.indexOf('{');
        int end   = text.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) return text.substring(start, end + 1);
        return "{}";
    }

    private UMLModel parseResponseToModel(String response) {
        List<UMLClass>    classes       = new ArrayList<>();
        List<Relationship> relationships = new ArrayList<>();

        try {
            String json = extractJsonBlock(response);
            JsonObject parsed = gson.fromJson(json, JsonObject.class);

            // Parse classes
            if (parsed.has("classes")) {
                JsonArray classArr = parsed.getAsJsonArray("classes");
                for (var c : classArr) {
                    JsonObject cObj = c.getAsJsonObject();
                    String name = cObj.has("name") ? cObj.get("name").getAsString() : "Unknown";
                    UMLClass cls = new UMLClass(0, name);
                    cls.setAbstract(cObj.has("isAbstract") && cObj.get("isAbstract").getAsBoolean());
                    cls.setInterface(cObj.has("isInterface") && cObj.get("isInterface").getAsBoolean());

                    // Parse fields
                    if (cObj.has("fields")) {
                        for (var f : cObj.getAsJsonArray("fields")) {
                            parseField(cls, f.getAsString());
                        }
                    }
                    // Parse methods
                    if (cObj.has("methods")) {
                        for (var m : cObj.getAsJsonArray("methods")) {
                            parseMethod(cls, m.getAsString());
                        }
                    }
                    classes.add(cls);
                }
            }

            // Parse relationships
            if (parsed.has("relationships")) {
                for (var r : parsed.getAsJsonArray("relationships")) {
                    JsonObject rObj = r.getAsJsonObject();
                    String src   = rObj.has("source") ? rObj.get("source").getAsString() : null;
                    String tgt   = rObj.has("target") ? rObj.get("target").getAsString() : null;
                    String type  = rObj.has("type")   ? rObj.get("type").getAsString()   : "ASSOCIATION";

                    UMLClass srcCls = classes.stream().filter(cl -> cl.getName().equals(src)).findFirst().orElse(null);
                    UMLClass tgtCls = classes.stream().filter(cl -> cl.getName().equals(tgt)).findFirst().orElse(null);
                    if (srcCls == null || tgtCls == null) continue;

                    Relationship rel = buildRelationship(type, srcCls, tgtCls);
                    if (rel != null) relationships.add(rel);
                }
            }
        } catch (Exception e) {
            System.err.println("[LLMAPIEngine] Parse error: " + e.getMessage());
        }

        return new UMLModel(classes, relationships, response);
    }

    private void parseField(UMLClass cls, String raw) {
        if (raw == null || raw.isBlank()) return;
        String s = raw.trim();
        Visibility vis = Visibility.PRIVATE;
        if (s.startsWith("+")) { vis = Visibility.PUBLIC;    s = s.substring(1).trim(); }
        else if (s.startsWith("-")) { vis = Visibility.PRIVATE;   s = s.substring(1).trim(); }
        else if (s.startsWith("#")) { vis = Visibility.PROTECTED; s = s.substring(1).trim(); }
        String name = s, type = "Object";
        if (s.contains(":")) {
            String[] parts = s.split(":", 2);
            name = parts[0].trim(); type = parts[1].trim();
        }
        cls.addAttribute(new Attribute(0, name, type, vis, false));
    }

    private void parseMethod(UMLClass cls, String raw) {
        if (raw == null || raw.isBlank()) return;
        String s = raw.trim();
        Visibility vis = Visibility.PUBLIC;
        if (s.startsWith("+")) { vis = Visibility.PUBLIC;    s = s.substring(1).trim(); }
        else if (s.startsWith("-")) { vis = Visibility.PRIVATE;   s = s.substring(1).trim(); }
        else if (s.startsWith("#")) { vis = Visibility.PROTECTED; s = s.substring(1).trim(); }
        String name = s, ret = "void";
        if (s.contains(":")) { ret = s.substring(s.lastIndexOf(":") + 1).trim();
                               s   = s.substring(0, s.lastIndexOf(":")).trim(); }
        name = s.contains("(") ? s.substring(0, s.indexOf("(")).trim() : s.trim();
        Method m = new Method(0, name, ret, vis, false);
        cls.addMethod(m);
    }

    private Relationship buildRelationship(String type, UMLClass src, UMLClass tgt) {
        try {
            RelationshipType rt = RelationshipType.valueOf(type.toUpperCase());
            return switch (rt) {
                case INHERITANCE -> new InheritanceRelationship(0, src, tgt, false);
                case REALIZATION -> new InheritanceRelationship(0, src, tgt, true);
                case COMPOSITION -> new AssociationRelationship(0, src, tgt, true,  false, Navigability.UNIDIRECTIONAL);
                case AGGREGATION -> new AssociationRelationship(0, src, tgt, false, true,  Navigability.UNIDIRECTIONAL);
                case DEPENDENCY  -> new DependencyRelationship(0, src, tgt, "uses");
                default          -> new AssociationRelationship(0, src, tgt, false, false, Navigability.UNIDIRECTIONAL);
            };
        } catch (IllegalArgumentException e) {
            return new AssociationRelationship(0, src, tgt, false, false, Navigability.UNIDIRECTIONAL);
        }
    }
}
