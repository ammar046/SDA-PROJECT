package com.umlytics.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.umlytics.domain.AssociationRelationship;
import com.umlytics.domain.Attribute;
import com.umlytics.domain.DependencyRelationship;
import com.umlytics.domain.EvaluationReport;
import com.umlytics.domain.InheritanceRelationship;
import com.umlytics.domain.Method;
import com.umlytics.domain.ProjectContext;
import com.umlytics.domain.Relationship;
import com.umlytics.domain.UMLClass;
import com.umlytics.domain.UMLModel;
import com.umlytics.enums.RelationshipType;
import com.umlytics.enums.Visibility;
import com.umlytics.exceptions.AIEngineException;
import com.umlytics.exceptions.ParseResponseException;
import com.umlytics.interfaces.IAIEngine;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

// GRASP: Pure Fabrication — isolates OpenAI-compatible HTTP calls from controllers.
public class LLMAPIEngine implements IAIEngine {
    private static final Gson GSON = new Gson();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final String UML_JSON_SYSTEM = """
            You are a UML class diagram expert. Reply with a single JSON object only (no markdown fences), using this exact shape:
            {
              "diagramTitle": "short title",
              "classes": [
                {
                  "name": "ClassName",
                  "isAbstract": false,
                  "isInterface": false,
                  "attributes": [{"name":"field","type":"String","visibility":"PRIVATE","static": false}],
                  "methods": [{"name":"doWork","returnType":"void","visibility":"PUBLIC","parameters":[],"abstract": false}]
                }
              ],
              "relationships": [
                {"type":"ASSOCIATION","source":"SourceClass","target":"TargetClass","label":""}
              ]
            }
            Allowed relationship type strings: ASSOCIATION, INHERITANCE, REALIZATION, COMPOSITION, AGGREGATION, DEPENDENCY.
            Visibility: PUBLIC, PRIVATE, PROTECTED, PACKAGE.
            Use plausible names from the user's domain. Keep the diagram small (typically 3–12 classes) unless the user asks for more.
            """;

    private String apiKey;
    private String apiEndpoint;
    private String modelName;
    private final OkHttpClient httpClient;

    public LLMAPIEngine() {
        loadConfig();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(45, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public UMLModel generateFromText(String desc) {
        if (desc == null || desc.isBlank()) {
            throw new AIEngineException("Description cannot be empty.", null);
        }
        if (!apiConfigured()) {
            return offlineTextStub(desc);
        }
        String user = "Design request:\n" + desc.trim();
        String content = chatCompletion(UML_JSON_SYSTEM, user);
        try {
            return parseUMLModelJson(stripMarkdownFence(content));
        } catch (Exception e) {
            throw new ParseResponseException("Could not parse UML JSON from the model. Raw snippet: "
                    + content.substring(0, Math.min(200, content.length())), e);
        }
    }

    @Override
    public EvaluationReport evaluateDesign(UMLModel m) {
        if (!apiConfigured()) {
            return offlineEvaluationStub(m);
        }
        String system = """
                You are a senior software architect. Given a UML class diagram as JSON, rate coupling, cohesion, and SOLID adherence from 0–10 (10 best).
                Reply with JSON only: {"couplingScore":n,"cohesionScore":n,"solidScore":n,"suggestions":["...","..."]}
                Each suggestion one short actionable sentence.
                """;
        String user = "Diagram JSON:\n" + summarizeModelJson(m);
        String content = chatCompletion(system, user);
        return parseEvaluationReport(stripMarkdownFence(content));
    }

    @Override
    public String consultDesign(String q, ProjectContext ctx) {
        String context = ctx == null ? "" : ctx.buildContextPrompt();
        if (!apiConfigured()) {
            return "AI guidance for query: " + q + "\nContext: " + context + "\n\n(Configure ai.api.key in config.properties for live LLM answers.)";
        }
        String system = """
                You are a helpful UML / object-oriented design assistant. Answer concisely with practical advice.
                Prefer bullet points when listing multiple items.
                """;
        String user = "Context: " + context + "\n\nUser question:\n" + q;
        return chatCompletion(system, user);
    }

    @Override
    public String generateStructure(UMLModel m) {
        if (!apiConfigured()) {
            return offlineStructureJson(m);
        }
        String system = """
                You output JSON only. Given a UML diagram description as JSON, produce Java skeleton classes.
                Shape: {"skeletons":{"ClassName":"full source code as a string","Other":"..."}}
                Use package-less public classes, sensible fields from attributes, method stubs from methods. No markdown.
                """;
        String user = "Diagram JSON:\n" + summarizeModelJson(m);
        String content = chatCompletion(system, user);
        String json = stripMarkdownFence(content);
        try {
            JsonElement el = JsonParser.parseString(json);
            if (el.isJsonObject() && el.getAsJsonObject().has("skeletons")) {
                return el.getAsJsonObject().toString();
            }
        } catch (Exception ignored) {
            // wrap below
        }
        return "{\"skeletons\":{\"Generated\":\"" + escapeForJson(json) + "\"}}";
    }

    @Override
    public UMLModel analyzeImage(byte[] data) {
        if (data == null || data.length == 0) {
            throw new AIEngineException("Uploaded image data is empty.", null);
        }
        if (!apiConfigured()) {
            UMLModel model = new UMLModel();
            model.setRawJson("{\"offline\":true,\"bytes\":" + data.length + "}");
            return model;
        }
        String mime = sniffImageMime(data);
        String b64 = Base64.getEncoder().encodeToString(data);
        String dataUrl = "data:" + mime + ";base64," + b64;

        JsonArray content = new JsonArray();
        JsonObject textPart = new JsonObject();
        textPart.addProperty("type", "text");
        textPart.addProperty("text", "This image should depict a UML class diagram. " + UML_JSON_SYSTEM);
        content.add(textPart);
        JsonObject imgPart = new JsonObject();
        imgPart.addProperty("type", "image_url");
        JsonObject urlObj = new JsonObject();
        urlObj.addProperty("url", dataUrl);
        imgPart.add("image_url", urlObj);
        content.add(imgPart);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.add("content", content);

        String contentStr = chatCompletionWithMessages(List.of(userMsg));
        try {
            return parseUMLModelJson(stripMarkdownFence(contentStr));
        } catch (Exception e) {
            throw new ParseResponseException("Could not parse UML from image analysis.", e);
        }
    }

    private boolean apiConfigured() {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }
        if (apiKey.trim().equalsIgnoreCase("YOUR_API_KEY_HERE")) {
            return false;
        }
        return apiEndpoint != null && !apiEndpoint.isBlank();
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
        apiKey = properties.getProperty("ai.api.key", "").trim();
        apiEndpoint = properties.getProperty("ai.api.endpoint", "").trim();
        modelName = properties.getProperty("ai.api.model", "gpt-4o").trim();
    }

    private String chatCompletion(String system, String user) {
        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", system);
        JsonObject usr = new JsonObject();
        usr.addProperty("role", "user");
        usr.addProperty("content", user);
        return chatCompletionWithMessages(List.of(sys, usr));
    }

    private String chatCompletionWithMessages(List<JsonObject> messages) {
        JsonObject body = new JsonObject();
        body.addProperty("model", modelName);
        JsonArray arr = new JsonArray();
        for (JsonObject m : messages) {
            arr.add(m);
        }
        body.add("messages", arr);
        body.addProperty("temperature", 0.25);

        Request request = new Request.Builder()
                .url(apiEndpoint)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new AIEngineException("LLM API error HTTP " + response.code() + ": " + respBody, null);
            }
            return extractAssistantText(respBody);
        } catch (AIEngineException e) {
            throw e;
        } catch (Exception e) {
            throw new AIEngineException("LLM request failed: " + e.getMessage(), e);
        }
    }

    private static String extractAssistantText(String responseBody) {
        JsonObject root;
        try {
            root = JsonParser.parseString(responseBody).getAsJsonObject();
        } catch (Exception e) {
            throw new ParseResponseException("Invalid API JSON.", e);
        }
        if (!root.has("choices") || root.getAsJsonArray("choices").size() == 0) {
            throw new AIEngineException("API returned no choices.", null);
        }
        JsonObject choice = root.getAsJsonArray("choices").get(0).getAsJsonObject();
        JsonObject message = choice.getAsJsonObject("message");
        if (!message.has("content") || message.get("content").isJsonNull()) {
            throw new AIEngineException("Empty assistant message.", null);
        }
        return message.get("content").getAsString();
    }

    private static String stripMarkdownFence(String text) {
        if (text == null) {
            return "";
        }
        String s = text.trim();
        if (s.startsWith("```")) {
            int firstNl = s.indexOf('\n');
            int lastFence = s.lastIndexOf("```");
            if (firstNl > 0 && lastFence > firstNl) {
                s = s.substring(firstNl + 1, lastFence).trim();
            }
        }
        return s;
    }

    private static UMLModel parseUMLModelJson(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        UMLModel model = new UMLModel();
        model.setRawJson(json);

        Map<String, UMLClass> byName = new HashMap<>();
        int cid = 1;
        int ax = 0;
        int ay = 0;

        JsonArray classes = root.has("classes") && root.get("classes").isJsonArray()
                ? root.getAsJsonArray("classes") : new JsonArray();
        for (JsonElement el : classes) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject o = el.getAsJsonObject();
            String name = getStr(o, "name");
            if (name == null || name.isBlank()) {
                continue;
            }
            UMLClass c = new UMLClass();
            c.setClassId(cid++);
            c.setName(name.trim());
            c.setAbstract(getBool(o, "isAbstract", false));
            c.setInterface(getBool(o, "isInterface", false));
            c.setPositionX(80 + (ax % 4) * 220);
            c.setPositionY(80 + (ay / 4) * 160);
            ax++;
            ay++;

            if (o.has("attributes") && o.get("attributes").isJsonArray()) {
                int aid = 1;
                for (JsonElement ae : o.getAsJsonArray("attributes")) {
                    if (!ae.isJsonObject()) {
                        continue;
                    }
                    JsonObject ao = ae.getAsJsonObject();
                    Attribute a = new Attribute();
                    a.setAttributeId(aid++);
                    a.setName(getStr(ao, "name", "field"));
                    a.setType(getStr(ao, "type", "Object"));
                    a.setVisibility(parseVisibility(getStr(ao, "visibility", "PRIVATE")));
                    a.setStatic(getBool(ao, "static", false));
                    c.addAttribute(a);
                }
            }
            if (o.has("methods") && o.get("methods").isJsonArray()) {
                int mid = 1;
                for (JsonElement me : o.getAsJsonArray("methods")) {
                    if (!me.isJsonObject()) {
                        continue;
                    }
                    JsonObject mo = me.getAsJsonObject();
                    Method m = new Method();
                    m.setMethodId(mid++);
                    m.setName(getStr(mo, "name", "method"));
                    m.setReturnType(getStr(mo, "returnType", "void"));
                    m.setVisibility(parseVisibility(getStr(mo, "visibility", "PUBLIC")));
                    m.setAbstract(getBool(mo, "abstract", false));
                    if (mo.has("parameters") && mo.get("parameters").isJsonArray()) {
                        List<String> ps = new ArrayList<>();
                        for (JsonElement pe : mo.getAsJsonArray("parameters")) {
                            ps.add(pe.isJsonPrimitive() ? pe.getAsString() : pe.toString());
                        }
                        m.setParameters(ps);
                    }
                    c.addMethod(m);
                }
            }
            byName.put(c.getName(), c);
            model.getClasses().add(c);
        }

        JsonArray rels = root.has("relationships") && root.get("relationships").isJsonArray()
                ? root.getAsJsonArray("relationships") : new JsonArray();
        int rid = 1;
        for (JsonElement el : rels) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject o = el.getAsJsonObject();
            String typeStr = getStr(o, "type", "ASSOCIATION");
            String srcName = getStr(o, "source");
            String tgtName = getStr(o, "target");
            UMLClass src = srcName != null ? byName.get(srcName.trim()) : null;
            UMLClass tgt = tgtName != null ? byName.get(tgtName.trim()) : null;
            if (src == null || tgt == null) {
                continue;
            }
            RelationshipType rt;
            try {
                rt = RelationshipType.valueOf(typeStr == null ? "ASSOCIATION" : typeStr.trim().toUpperCase());
            } catch (Exception e) {
                rt = RelationshipType.ASSOCIATION;
            }
            Relationship r = switch (rt) {
                case INHERITANCE -> {
                    InheritanceRelationship x = new InheritanceRelationship();
                    x.setInterface(false);
                    yield x;
                }
                case REALIZATION -> {
                    InheritanceRelationship x = new InheritanceRelationship();
                    x.setInterface(true);
                    yield x;
                }
                case COMPOSITION -> {
                    AssociationRelationship x = new AssociationRelationship();
                    x.setComposition(true);
                    x.setAggregation(false);
                    x.setNavigability(com.umlytics.enums.Navigability.BIDIRECTIONAL);
                    yield x;
                }
                case AGGREGATION -> {
                    AssociationRelationship x = new AssociationRelationship();
                    x.setComposition(false);
                    x.setAggregation(true);
                    x.setNavigability(com.umlytics.enums.Navigability.BIDIRECTIONAL);
                    yield x;
                }
                case DEPENDENCY -> new DependencyRelationship();
                case ASSOCIATION -> {
                    AssociationRelationship x = new AssociationRelationship();
                    x.setComposition(false);
                    x.setAggregation(false);
                    x.setNavigability(com.umlytics.enums.Navigability.BIDIRECTIONAL);
                    yield x;
                }
                default -> {
                    AssociationRelationship x = new AssociationRelationship();
                    x.setComposition(false);
                    x.setAggregation(false);
                    x.setNavigability(com.umlytics.enums.Navigability.BIDIRECTIONAL);
                    yield x;
                }
            };
            r.setRelationshipId(rid++);
            r.setSourceClass(src);
            r.setTargetClass(tgt);
            r.setLabel(getStr(o, "label", ""));
            model.getRelationships().add(r);
        }

        return model;
    }

    private static Visibility parseVisibility(String v) {
        if (v == null || v.isBlank()) {
            return Visibility.PRIVATE;
        }
        try {
            return Visibility.valueOf(v.trim().toUpperCase());
        } catch (Exception e) {
            return Visibility.PRIVATE;
        }
    }

    private static EvaluationReport parseEvaluationReport(String json) {
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            EvaluationReport r = new EvaluationReport();
            r.setCouplingScore(o.has("couplingScore") ? clampScore(o.get("couplingScore").getAsDouble()) : 5);
            r.setCohesionScore(o.has("cohesionScore") ? clampScore(o.get("cohesionScore").getAsDouble()) : 5);
            r.setSolidScore(o.has("solidScore") ? clampScore(o.get("solidScore").getAsDouble()) : 5);
            List<String> suggestions = new ArrayList<>();
            if (o.has("suggestions") && o.get("suggestions").isJsonArray()) {
                for (JsonElement e : o.getAsJsonArray("suggestions")) {
                    suggestions.add(e.getAsString());
                }
            }
            if (suggestions.isEmpty()) {
                suggestions.add("Review extracted scores; add more detail to the diagram for finer feedback.");
            }
            r.setSuggestions(suggestions);
            return r;
        } catch (Exception e) {
            EvaluationReport r = new EvaluationReport();
            r.setCouplingScore(5);
            r.setCohesionScore(5);
            r.setSolidScore(5);
            r.setSuggestions(List.of("Could not parse evaluation JSON from model.", json.substring(0, Math.min(300, json.length()))));
            return r;
        }
    }

    private static double clampScore(double v) {
        return Math.max(0, Math.min(10, v));
    }

    private static String summarizeModelJson(UMLModel m) {
        JsonObject root = new JsonObject();
        root.addProperty("classCount", m.getClasses() == null ? 0 : m.getClasses().size());
        JsonArray classes = new JsonArray();
        if (m.getClasses() != null) {
            for (UMLClass c : m.getClasses()) {
                JsonObject jo = new JsonObject();
                jo.addProperty("name", c.getName());
                jo.addProperty("isAbstract", c.isAbstract());
                jo.addProperty("isInterface", c.isInterface());
                classes.add(jo);
            }
        }
        root.add("classes", classes);
        JsonArray rels = new JsonArray();
        if (m.getRelationships() != null) {
            for (Relationship r : m.getRelationships()) {
                JsonObject jo = new JsonObject();
                jo.addProperty("type", r.getType().name());
                jo.addProperty("source", r.getSource() != null ? r.getSource().getName() : "");
                jo.addProperty("target", r.getTarget() != null ? r.getTarget().getName() : "");
                rels.add(jo);
            }
        }
        root.add("relationships", rels);
        if (m.getRawJson() != null && !m.getRawJson().isBlank()) {
            root.addProperty("rawSnippet", m.getRawJson().substring(0, Math.min(4000, m.getRawJson().length())));
        }
        return GSON.toJson(root);
    }

    private static String offlineStructureJson(UMLModel m) {
        Map<String, String> sk = new LinkedHashMap<>();
        if (m.getClasses() == null || m.getClasses().isEmpty()) {
            sk.put("Placeholder", "// Configure ai.api.key for live structure generation.\npublic class Placeholder {\n}\n");
        } else {
            for (UMLClass c : m.getClasses()) {
                String n = c.getName() == null ? "Untitled" : c.getName();
                sk.put(n, "public class " + n + " {\n  // offline skeleton\n}\n");
            }
        }
        JsonObject wrap = new JsonObject();
        JsonObject skel = new JsonObject();
        for (Map.Entry<String, String> e : sk.entrySet()) {
            skel.addProperty(e.getKey(), e.getValue());
        }
        wrap.add("skeletons", skel);
        return wrap.toString();
    }

    private static UMLModel offlineTextStub(String desc) {
        UMLModel model = new UMLModel();
        model.setRawJson("{\"input\":\"" + escapeForJson(desc) + "\",\"offline\":true}");
        return model;
    }

    private static EvaluationReport offlineEvaluationStub(UMLModel m) {
        int n = m.getClasses() == null ? 0 : m.getClasses().size();
        EvaluationReport report = new EvaluationReport();
        report.setCouplingScore(6.0);
        report.setCohesionScore(6.5);
        report.setSolidScore(6.0);
        List<String> s = new ArrayList<>();
        s.add("Offline mode: set ai.api.key in config.properties for LLM-based evaluation.");
        s.add("Diagram has " + n + " class(es). Refine responsibilities and dependencies.");
        report.setSuggestions(s);
        return report;
    }

    private static String sniffImageMime(byte[] data) {
        if (data.length >= 4 && data[0] == (byte) 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47) {
            return "image/png";
        }
        if (data.length >= 3 && data[0] == (byte) 0xFF && data[1] == (byte) 0xD8) {
            return "image/jpeg";
        }
        return "image/png";
    }

    private static String escapeForJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String getStr(JsonObject o, String key) {
        return getStr(o, key, null);
    }

    private static String getStr(JsonObject o, String key, String def) {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            return def;
        }
        return o.get(key).getAsString();
    }

    private static boolean getBool(JsonObject o, String key, boolean def) {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            return def;
        }
        try {
            return o.get(key).getAsBoolean();
        } catch (Exception e) {
            return def;
        }
    }
}
