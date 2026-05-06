package com.umlytics.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.umlytics.domain.AssociationRelationship;
import com.umlytics.domain.Attribute;
import com.umlytics.domain.ChatMessage;
import com.umlytics.domain.ConceptualClass;
import com.umlytics.domain.DependencyRelationship;
import com.umlytics.domain.DesignEvaluationReport;
import com.umlytics.domain.InheritanceRelationship;
import com.umlytics.domain.Method;
import com.umlytics.domain.ProjectContext;
import com.umlytics.domain.Relationship;
import com.umlytics.domain.UMLDiagram;
import com.umlytics.domain.UMLModel;
import com.umlytics.enums.ClassType;
import com.umlytics.enums.RelationshipType;
import com.umlytics.enums.Visibility;
import com.umlytics.exceptions.AIEngineException;
import com.umlytics.interfaces.IAIEngine;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

// GRASP: Pure Fabrication — OpenAI-compatible Chat Completions (text + vision).
public class LLMAPIEngine implements IAIEngine {

    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    private static final String SYSTEM_UML_JSON = """
            You are a UML class-diagram assistant. Reply with ONLY valid JSON (no markdown fences), matching this schema:
            {"classes":[{"name":"string","isAbstract":false,"isInterface":false,"x":100,"y":100,"attributes":[{"name":"","type":"","visibility":"PRIVATE"}],"methods":[{"name":"","returnType":"","parameters":"","visibility":"PUBLIC"}]}],"relationships":[{"sourceClass":"","targetClass":"","type":"ASSOCIATION","sourceMultiplicity":"1","targetMultiplicity":"1","label":""}]}
            Allowed relationship type strings: ASSOCIATION, AGGREGATION, COMPOSITION, INHERITANCE, REALIZATION, DEPENDENCY.
            For INHERITANCE and REALIZATION: sourceClass is the subtype (implements/extends), targetClass is the supertype or interface.
            Infer reasonable classes and relationships from the user input or image.""";

    /** Must NOT reuse SYSTEM_UML_JSON — that asks for diagram JSON and breaks Java skeleton generation. */
    private static final String SYSTEM_JAVA_SKELETON_JSON = """
            You output compilable Java 17 skeleton code wrapped in JSON only. Reply with a single flat JSON object, no markdown fences.
            Shape: {"skeletons":{"ClassName":"MULTI-LINE JAVA SOURCE STRING"}}
            Critical rules:
            - Keys in "skeletons" must be EXACTLY the class/interface/enum names listed in the user message — no extra types, no renamed types.
            - Each value is ONE string containing real Java source. The first lines must be valid Java: optional `package ...;` lines, then `import ...;` lines, then `public class|interface|enum`.
            - NEVER put JSON objects, JSON maps, or pseudo-structures like {"package":"..."} inside a value. NEVER use a top-level "type":"object" wrapper around skeletons.
            - Match fields and method signatures to the UML attributes and methods from the user JSON.
            - Do not include explanations outside the JSON object.""";

    /** Chat / design Q&A — never use SYSTEM_UML_JSON here or the model returns diagram JSON for every question. */
    private static final String SYSTEM_DESIGN_CONSULT = """
            You are an expert software design tutor. Help with UML class diagrams, SOLID (SRP, OCP, LSP, ISP, DIP), coupling, cohesion, and object-oriented design.

            Answer in clear, readable English. Use short paragraphs and bullet points when they help. Be specific to the user's question—different questions must get different answers.

            Rules:
            - When a diagram summary (JSON) is provided, reference concrete class names, attributes, methods, and relationships from it.
            - If no diagram is attached, explain concepts generally without inventing a fake diagram.
            - Never reply with raw UML export JSON, schema templates, or lines like {"classes":[...]} unless the user explicitly asks for JSON output.
            - Do not describe or invent a minimal placeholder diagram (e.g. a single "PotentialProblem" class) unless it reflects the given diagram.""";

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build();

    private final String apiKey;
    private final String endpoint;
    private final String modelName;
    /** Optional OpenAI-compatible vision endpoint when the primary API is text-only (e.g. Cerebras). */
    private final String visionEndpoint;
    private final String visionApiKey;
    private final String visionModel;
    /** Optional fixed seed for evaluation reproducibility (OpenAI-compatible). */
    private final Integer evalSeed;
    /** Google Gemini (Generative Language API) — set ai.provider=gemini or only configure a Gemini key. */
    private final String geminiApiKey;
    private final String geminiModel;
    private final boolean useGemini;

    public LLMAPIEngine() {
        Properties props = loadProps();
        this.apiKey = resolveOpenAiCompatibleApiKey(props);
        this.endpoint = props.getProperty("ai.api.endpoint", "https://api.openai.com/v1/chat/completions");
        this.modelName = props.getProperty("ai.api.model", "gpt-4o");
        this.visionEndpoint = trimOrEmpty(props.getProperty("ai.vision.endpoint"));
        this.visionApiKey = trimOrEmpty(props.getProperty("ai.vision.api.key"));
        this.visionModel = trimOrEmpty(props.getProperty("ai.vision.model"));
        this.evalSeed = parseIntOrNull(trimOrEmpty(props.getProperty("ai.eval.seed")));
        String gemini = trimOrEmpty(props.getProperty("ai.gemini.api.key"));
        if (gemini.isEmpty()) {
            gemini = trimOrEmpty(System.getenv("GEMINI_API_KEY"));
        }
        this.geminiApiKey = gemini;
        String gModel = trimOrEmpty(props.getProperty("ai.gemini.model"));
        this.geminiModel = gModel.isEmpty() ? "gemini-2.0-flash" : gModel;
        String provider = trimOrEmpty(props.getProperty("ai.provider", "")).toLowerCase();
        boolean openAiReady = openAiCredentialsLookValid(this.apiKey);
        this.useGemini = ("gemini".equals(provider) && !this.geminiApiKey.isEmpty())
                || (!this.geminiApiKey.isEmpty() && !openAiReady);
    }

    private static boolean openAiCredentialsLookValid(String key) {
        return !key.isEmpty()
                && !key.equalsIgnoreCase("YOUR_API_KEY_HERE")
                && !key.startsWith("${");
    }

    /**
     * Uses {@code ai.api.key} from config when valid; otherwise {@code UMLYTICS_API_KEY}, {@code OPENAI_API_KEY},
     * or {@code CEREBRAS_API_KEY} from the environment (OpenAI-compatible providers including Cerebras).
     */
    private static String resolveOpenAiCompatibleApiKey(Properties props) {
        String fromFile = trimOrEmpty(props.getProperty("ai.api.key"));
        if (openAiCredentialsLookValid(fromFile)) {
            return fromFile;
        }
        for (String env : new String[] { "UMLYTICS_API_KEY", "OPENAI_API_KEY", "CEREBRAS_API_KEY" }) {
            String v = trimOrEmpty(System.getenv(env));
            if (openAiCredentialsLookValid(v)) {
                return v;
            }
        }
        return fromFile;
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Properties loadProps() {
        Properties merged = new Properties();
        try (InputStream in = LLMAPIEngine.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                merged.load(in);
            }
        } catch (IOException ignored) {
            // leave empty
        }
        // Overlay project-root config (local secrets). Skipped during Maven tests — see pom.xml surefire env.
        boolean skipLocalOverlay = Boolean.parseBoolean(System.getenv("UMLYTICS_SKIP_LOCAL_CONFIG"))
                || Boolean.parseBoolean(System.getProperty("umlytics.skip.local.config", "false"));
        Path[] candidates = new Path[] {
                Path.of(System.getProperty("user.dir", ".")).resolve("config.properties")
        };
        if (skipLocalOverlay) {
            return merged;
        }
        for (Path path : candidates) {
            if (Files.isRegularFile(path)) {
                try (InputStream in = Files.newInputStream(path)) {
                    Properties overlay = new Properties();
                    overlay.load(in);
                    merged.putAll(overlay);
                } catch (IOException ignored) {
                    // skip this file
                }
            }
        }
        return merged;
    }

    private static String trimOrEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private boolean apiConfigured() {
        if (useGemini) {
            return !geminiApiKey.isEmpty()
                    && !geminiApiKey.equalsIgnoreCase("YOUR_GEMINI_API_KEY_HERE")
                    && !geminiApiKey.startsWith("${");
        }
        return openAiCredentialsLookValid(apiKey);
    }

    private void requireApi() {
        if (!apiConfigured()) {
            throw new AIEngineException(
                    "AI is not configured. Set ai.api.key for OpenAI-compatible APIs, or ai.gemini.api.key (or GEMINI_API_KEY) "
                            + "with ai.provider=gemini in config.properties.",
                    null);
        }
    }

    @Override
    public UMLModel generateFromText(String desc) {
        if (desc == null || desc.isBlank()) {
            throw new AIEngineException("Description cannot be empty.", null);
        }
        requireApi();
        String user = "Create a UML class diagram for this system description:\n" + desc.trim();
        String content = callChatCompletions(user, null);
        return parseUmlModelFromJson(content);
    }

    @Override
    public DesignEvaluationReport evaluateDesign(UMLModel m) {
        requireApi();
        String payload = m.getRawJson() != null ? m.getRawJson() : AiDiagramPayload.summarizeModelAsJson(m);
        // Leave room for class list + prompt; large diagrams may truncate — names list is repeated up front.
        payload = truncate(payload, 10_000);
        String allowed = AiDiagramPayload.allowedNamesForPrompt(m);
        // Do not show example scores of 0 — models often copy them literally.
        String user = """
                Allowed UML type names (from this diagram only). Your "summary" and EVERY item in "suggestions" MUST cite at least one of these names (exact spelling):
                """ + allowed + """

                The JSON below is the ONLY model to grade. Do not invent classes (e.g. from other projects). Do not substitute a generic system.

                Evaluate coupling (lower is better), cohesion (higher is better), and SOLID alignment (higher is better).

                Respond with ONE flat JSON object only (no JSON Schema "type"/"properties" wrapper).
                Keys required: "couplingScore", "cohesionScore", "solidScore", "suggestions" (array of short strings), "summary" (2–5 sentences).
                Scores: decimals 1.0–10.0, justified by this diagram.

                Diagram JSON:
                """ + payload;
        String content = callEvaluationChat(user);
        return parseEvaluationJson(content, m);
    }

    @Override
    public String consultDesign(String q, ProjectContext ctx) {
        if (q == null || q.isBlank()) {
            return "";
        }
        if (!apiConfigured()) {
            return "Offline: configure ai.api.key or ai.gemini.api.key in config.properties for AI answers. You asked: " + q.trim();
        }
        String userPayload = buildConsultUserPayload(q.trim(), ctx);
        return callConsultChat(userPayload);
    }

    /**
     * Keeps total request size within typical 8k-token context limits (input + system prompt).
     */
    private static final int MAX_CONSULT_DIAGRAM_JSON = 3_500;
    private static final int MAX_CONSULT_HISTORY_CHARS = 1_200;
    private static final int MAX_CONSULT_TOTAL_CHARS = 6_500;

    private String buildConsultUserPayload(String question, ProjectContext ctx) {
        final String questionBlock = "User question (answer this directly in plain English):\n" + question;

        StringBuilder sb = new StringBuilder();
        if (ctx != null && ctx.getCurrentDiagram() != null) {
            UMLDiagram d = ctx.getCurrentDiagram();
            UMLModel m = new UMLModel();
            m.setClasses(new ArrayList<>(d.getClasses()));
            m.setRelationships(new ArrayList<>(d.getRelationships()));
            sb.append("Current diagram title: ").append(d.getTitle() != null ? d.getTitle() : "(untitled)").append("\n");
            sb.append("UML summary (JSON — use only to understand the design, do not echo it as your answer):\n");
            sb.append(truncate(AiDiagramPayload.summarizeModelAsJson(m), MAX_CONSULT_DIAGRAM_JSON));
            sb.append("\n\n");
        } else {
            sb.append("(No diagram is open — answer from general software design knowledge.)\n\n");
        }
        if (ctx != null && ctx.getChatHistory() != null && !ctx.getChatHistory().isEmpty()) {
            sb.append("Recent chat (oldest first; may be truncated):\n");
            List<ChatMessage> hist = ctx.getChatHistory();
            int start = Math.max(0, hist.size() - 10);
            StringBuilder histBuf = new StringBuilder();
            for (int i = start; i < hist.size(); i++) {
                ChatMessage cm = hist.get(i);
                if (cm.getSender() == null || cm.getContent() == null) {
                    continue;
                }
                histBuf.append(cm.getSender().name()).append(": ")
                        .append(truncate(cm.getContent().trim(), 400)).append('\n');
                if (histBuf.length() >= MAX_CONSULT_HISTORY_CHARS) {
                    histBuf.append("…\n");
                    break;
                }
            }
            sb.append(truncate(histBuf.toString(), MAX_CONSULT_HISTORY_CHARS));
            sb.append("\n");
        }
        String prefix = sb.toString();
        int budget = MAX_CONSULT_TOTAL_CHARS - questionBlock.length();
        if (budget <= 0) {
            return questionBlock;
        }
        if (prefix.length() > budget) {
            prefix = truncate(prefix, budget);
        }
        return prefix + "\n" + questionBlock;
    }

    /** Design questions — must NOT use {@link #buildUmlChatBody} / {@link #SYSTEM_UML_JSON}. */
    private String callConsultChat(String userMessage) {
        JsonObject body = new JsonObject();
        body.addProperty("model", modelName);
        body.addProperty("temperature", 0.65);
        body.addProperty("stream", false);
        applyProviderTokenLimits(body, endpoint);
        applyReasoningEffort(body, modelName, endpoint);

        JsonArray messages = new JsonArray();
        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", SYSTEM_DESIGN_CONSULT);
        messages.add(sys);
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);
        body.add("messages", messages);

        return postChatCompletions(body, endpoint, apiKey);
    }

    @Override
    public String generateStructure(UMLModel m) {
        if (!apiConfigured()) {
            return "{\"skeletons\":{\"Note\":\"Set ai.api.key or ai.gemini.api.key in config.properties\"}}";
        }
        List<String> names = AiDiagramPayload.classNames(m);
        String exactKeys = names.isEmpty() ? "(none)" : String.join(", ", names);
        String modelJson = truncate(AiDiagramPayload.summarizeModelAsJson(m), 4_200);
        String user = """
                Output "skeletons" ONLY for these exact type names (spelling must match). One key per name, no extras:
                """ + exactKeys + """

                The model JSON below is the ONLY truth for attributes, methods, and relationships.

                Model JSON:
                """ + modelJson;
        String raw = callJavaSkeletonChat(user);
        if (raw == null || raw.isBlank()) {
            return "{\"skeletons\":{}}";
        }
        return SkeletonResponseNormalizer.toCleanSkeletonsJson(raw);
    }

    @Override
    public UMLModel analyzeImage(byte[] data) {
        if (data == null || data.length == 0) {
            throw new AIEngineException("Uploaded image data is empty.", null);
        }
        requireApi();
        byte[] imagePayload = compressDiagramImage(data);
        String user = "Analyze this UML class diagram image. Extract classes (with attributes and methods if visible) and relationships. "
                + "Use the JSON schema from your instructions.";
        try {
            String content = callChatCompletionsVision(user, imagePayload);
            return parseUmlModelFromJson(content);
        } catch (AIEngineException ex) {
            boolean hasDedicatedVision = !visionEndpoint.isEmpty() || !visionApiKey.isEmpty();
            if (!hasDedicatedVision && isCerebrasEndpoint()) {
                throw new AIEngineException(
                        "Image analysis needs a vision-capable API. Options: (1) Add ai.vision.endpoint=https://api.openai.com/v1/chat/completions "
                                + "with ai.vision.api.key and ai.vision.model=gpt-4o-mini (or gpt-4o), or (2) switch ai.api to a provider that supports "
                                + "image_url in chat. Underlying error: " + ex.getMessage(),
                        ex);
            }
            throw ex;
        }
    }

    private boolean isCerebrasEndpoint() {
        return endpoint != null && endpoint.toLowerCase().contains("cerebras");
    }

    /** Text-only UML generation / chat (same provider as main config). */
    private String callChatCompletions(String userText, byte[] imageBytes) {
        if (imageBytes != null) {
            return callChatCompletionsVision(userText, imageBytes);
        }
        JsonObject body = buildUmlChatBody(userText, null);
        applyProviderTokenLimits(body, endpoint);
        applyReasoningEffort(body, modelName, endpoint);
        return postChatCompletions(body, endpoint, apiKey);
    }

    /** Vision request: uses ai.vision.* when set, otherwise primary endpoint (same key/model). */
    private String callChatCompletionsVision(String userText, byte[] imageBytes) {
        String url = !visionEndpoint.isEmpty() ? visionEndpoint : endpoint;
        String key = !visionApiKey.isEmpty() ? visionApiKey : apiKey;
        String model = !visionModel.isEmpty() ? visionModel : modelName;
        JsonObject body = buildUmlChatBody(userText, imageBytes);
        body.addProperty("model", model);
        applyProviderTokenLimits(body, url);
        applyReasoningEffort(body, model, url);
        return postChatCompletions(body, url, key);
    }

    /** Design evaluation — strict JSON, json_object mode when supported. */
    private String callEvaluationChat(String userMessage) {
        JsonObject body = new JsonObject();
        body.addProperty("model", modelName);
        // Greedy decoding reduces score drift between runs; optional seed helps OpenAI-style APIs.
        body.addProperty("temperature", 0);
        body.addProperty("stream", false);
        if (evalSeed != null) {
            body.addProperty("seed", evalSeed);
        }
        applyProviderTokenLimits(body, endpoint);
        applyReasoningEffort(body, modelName, endpoint);

        JsonArray messages = new JsonArray();
        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content",
                "You are an expert software architect grading UML class diagrams. Reply with a single flat JSON object only "
                        + "(keys: couplingScore, cohesionScore, solidScore, suggestions, summary). No JSON Schema wrappers, no markdown.");
        messages.add(sys);
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);
        body.add("messages", messages);

        JsonObject rf = new JsonObject();
        rf.addProperty("type", "json_object");
        body.add("response_format", rf);

        try {
            return postChatCompletions(body, endpoint, apiKey);
        } catch (AIEngineException first) {
            body.remove("response_format");
            return postChatCompletions(body, endpoint, apiKey);
        }
    }

    /** Java skeleton generation — dedicated system prompt (not UML JSON schema). */
    private String callJavaSkeletonChat(String userMessage) {
        JsonObject body = new JsonObject();
        body.addProperty("model", modelName);
        body.addProperty("temperature", 0);
        body.addProperty("stream", false);
        applyProviderTokenLimits(body, endpoint);
        applyReasoningEffort(body, modelName, endpoint);

        JsonArray messages = new JsonArray();
        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", SYSTEM_JAVA_SKELETON_JSON);
        messages.add(sys);
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);
        body.add("messages", messages);

        JsonObject rf = new JsonObject();
        rf.addProperty("type", "json_object");
        body.add("response_format", rf);

        try {
            return postChatCompletions(body, endpoint, apiKey);
        } catch (AIEngineException first) {
            body.remove("response_format");
            return postChatCompletions(body, endpoint, apiKey);
        }
    }

    private JsonObject buildUmlChatBody(String userText, byte[] imageBytes) {
        JsonObject body = new JsonObject();
        body.addProperty("model", modelName);
        body.addProperty("temperature", 0.2);
        body.addProperty("stream", false);

        JsonArray messages = new JsonArray();
        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", SYSTEM_UML_JSON);
        messages.add(sys);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        if (imageBytes == null) {
            userMsg.addProperty("content", userText);
        } else {
            JsonArray parts = new JsonArray();
            JsonObject textPart = new JsonObject();
            textPart.addProperty("type", "text");
            textPart.addProperty("text", userText);
            parts.add(textPart);
            JsonObject imgPart = new JsonObject();
            imgPart.addProperty("type", "image_url");
            JsonObject urlObj = new JsonObject();
            String mime = detectImageMime(imageBytes);
            String b64 = Base64.getEncoder().encodeToString(imageBytes);
            urlObj.addProperty("url", "data:" + mime + ";base64," + b64);
            imgPart.add("image_url", urlObj);
            parts.add(imgPart);
            userMsg.add("content", parts);
        }
        messages.add(userMsg);
        body.add("messages", messages);
        return body;
    }

    private void applyProviderTokenLimits(JsonObject body, String url) {
        if (url != null && url.toLowerCase().contains("cerebras")) {
            body.addProperty("max_completion_tokens", -1);
        } else {
            body.addProperty("max_completion_tokens", 8192);
        }
    }

    private void applyReasoningEffort(JsonObject body, String model, String url) {
        if (model != null && model.toLowerCase().contains("gpt-oss")
                && url != null && url.toLowerCase().contains("cerebras")) {
            body.addProperty("reasoning_effort", "low");
        }
    }

    private String postChatCompletions(JsonObject body, String url, String bearerKey) {
        if (useGemini) {
            JsonObject geminiBody = convertOpenAiChatRequestToGemini(body);
            String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + geminiModel + ":generateContent?key=" + geminiApiKey;
            Request request = new Request.Builder()
                    .url(geminiUrl)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(geminiBody.toString(), JSON_MEDIA))
                    .build();
            try (Response resp = http.newCall(request).execute()) {
                String respBody = resp.body() != null ? resp.body().string() : "";
                if (!resp.isSuccessful()) {
                    String detail = httpErrorDetail(respBody);
                    throw new AIEngineException(
                            "Gemini request failed: HTTP " + resp.code()
                                    + (detail != null ? " — " + detail : " — " + truncate(respBody, 500)),
                            null);
                }
                return extractGeminiText(respBody);
            } catch (IOException e) {
                throw new AIEngineException("Gemini request failed: " + e.getMessage(), e);
            }
        }

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + bearerKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON_MEDIA))
                .build();

        try (Response resp = http.newCall(request).execute()) {
            String respBody = resp.body() != null ? resp.body().string() : "";
            if (!resp.isSuccessful()) {
                String detail = httpErrorDetail(respBody);
                throw new AIEngineException(
                        "AI request failed: HTTP " + resp.code() + (detail != null ? " — " + detail : " — " + truncate(respBody, 500)),
                        null);
            }
            return extractAssistantText(respBody);
        } catch (IOException e) {
            throw new AIEngineException("AI request failed: " + e.getMessage(), e);
        }
    }

    private static JsonObject convertOpenAiChatRequestToGemini(JsonObject openAiBody) {
        JsonObject out = new JsonObject();
        JsonObject genCfg = new JsonObject();
        if (openAiBody.has("temperature") && !openAiBody.get("temperature").isJsonNull()) {
            genCfg.add("temperature", openAiBody.get("temperature"));
        }
        if (openAiBody.has("max_completion_tokens") && openAiBody.get("max_completion_tokens").isJsonPrimitive()) {
            int m = openAiBody.get("max_completion_tokens").getAsInt();
            if (m > 0) {
                genCfg.addProperty("maxOutputTokens", Math.min(m, 8192));
            }
        }
        if (openAiBody.has("response_format") && openAiBody.get("response_format").isJsonObject()) {
            JsonObject rf = openAiBody.getAsJsonObject("response_format");
            if (rf.has("type") && "json_object".equals(rf.get("type").getAsString())) {
                genCfg.addProperty("responseMimeType", "application/json");
            }
        }
        out.add("generationConfig", genCfg);

        JsonArray messages = openAiBody.getAsJsonArray("messages");
        JsonObject systemInst = null;
        JsonArray contents = new JsonArray();
        for (JsonElement el : messages) {
            JsonObject m = el.getAsJsonObject();
            String role = m.get("role").getAsString();
            JsonElement contentEl = m.get("content");
            if ("system".equals(role)) {
                String t = extractOpenAiTextContent(contentEl);
                systemInst = new JsonObject();
                JsonArray sparts = new JsonArray();
                JsonObject pt = new JsonObject();
                pt.addProperty("text", t);
                sparts.add(pt);
                systemInst.add("parts", sparts);
            } else if ("user".equals(role)) {
                JsonObject row = new JsonObject();
                row.addProperty("role", "user");
                row.add("parts", geminiPartsFromUserContent(contentEl));
                contents.add(row);
            } else if ("assistant".equals(role)) {
                JsonObject row = new JsonObject();
                row.addProperty("role", "model");
                JsonArray parts = new JsonArray();
                JsonObject pt = new JsonObject();
                pt.addProperty("text", extractOpenAiTextContent(contentEl));
                parts.add(pt);
                row.add("parts", parts);
                contents.add(row);
            }
        }
        if (systemInst != null) {
            out.add("systemInstruction", systemInst);
        }
        out.add("contents", contents);
        return out;
    }

    private static String extractOpenAiTextContent(JsonElement contentEl) {
        if (contentEl == null || contentEl.isJsonNull()) {
            return "";
        }
        if (contentEl.isJsonPrimitive()) {
            return contentEl.getAsString();
        }
        if (contentEl.isJsonArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonElement e : contentEl.getAsJsonArray()) {
                if (e.isJsonObject()) {
                    JsonObject o = e.getAsJsonObject();
                    if (o.has("type") && "text".equals(o.get("type").getAsString()) && o.has("text")) {
                        sb.append(o.get("text").getAsString());
                    }
                }
            }
            return sb.toString();
        }
        return "";
    }

    private static JsonArray geminiPartsFromUserContent(JsonElement contentEl) {
        JsonArray parts = new JsonArray();
        if (contentEl == null || contentEl.isJsonNull()) {
            return parts;
        }
        if (contentEl.isJsonPrimitive()) {
            JsonObject p = new JsonObject();
            p.addProperty("text", contentEl.getAsString());
            parts.add(p);
            return parts;
        }
        if (contentEl.isJsonArray()) {
            for (JsonElement e : contentEl.getAsJsonArray()) {
                if (!e.isJsonObject()) {
                    continue;
                }
                JsonObject o = e.getAsJsonObject();
                String type = o.has("type") ? o.get("type").getAsString() : "text";
                if ("text".equals(type) && o.has("text")) {
                    JsonObject p = new JsonObject();
                    p.addProperty("text", o.get("text").getAsString());
                    parts.add(p);
                } else if ("image_url".equals(type) && o.has("image_url")) {
                    JsonObject iu = o.getAsJsonObject("image_url");
                    if (!iu.has("url")) {
                        continue;
                    }
                    String url = iu.get("url").getAsString();
                    int comma = url.indexOf(',');
                    String b64 = comma >= 0 ? url.substring(comma + 1) : url;
                    String mime = "image/png";
                    if (url.startsWith("data:")) {
                        int semi = url.indexOf(';');
                        if (semi > 5) {
                            mime = url.substring(5, semi);
                        }
                    }
                    JsonObject inline = new JsonObject();
                    inline.addProperty("mime_type", mime);
                    inline.addProperty("data", b64);
                    JsonObject p = new JsonObject();
                    p.add("inline_data", inline);
                    parts.add(p);
                }
            }
        }
        return parts;
    }

    private static String extractGeminiText(String responseJson) {
        JsonObject root = JsonParser.parseString(responseJson).getAsJsonObject();
        if (root.has("error")) {
            JsonElement errEl = root.get("error");
            String msg = errEl.isJsonObject() && errEl.getAsJsonObject().has("message")
                    ? errEl.getAsJsonObject().get("message").getAsString()
                    : errEl.toString();
            throw new AIEngineException("Gemini error: " + msg, null);
        }
        if (!root.has("candidates") || root.get("candidates").isJsonNull()) {
            throw new AIEngineException("Gemini returned no candidates. Raw: " + truncate(responseJson, 400), null);
        }
        JsonArray candidates = root.getAsJsonArray("candidates");
        if (candidates.size() == 0) {
            throw new AIEngineException("Gemini returned no candidates. Raw: " + truncate(responseJson, 400), null);
        }
        JsonObject cand = candidates.get(0).getAsJsonObject();
        if (cand.has("content") && !cand.get("content").isJsonNull()) {
            JsonObject content = cand.getAsJsonObject("content");
            if (content.has("parts")) {
                StringBuilder sb = new StringBuilder();
                for (JsonElement p : content.getAsJsonArray("parts")) {
                    if (p.isJsonObject() && p.getAsJsonObject().has("text")) {
                        sb.append(p.getAsJsonObject().get("text").getAsString());
                    }
                }
                String text = sb.toString().trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        throw new AIEngineException("Gemini returned empty text. Raw: " + truncate(responseJson, 400), null);
    }

    /** Resize/compress large diagrams so providers accept the payload. */
    private static byte[] compressDiagramImage(byte[] data) {
        if (data == null || data.length < 120_000) {
            return data;
        }
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(data));
            if (src == null) {
                return data;
            }
            final int maxSide = 1536;
            int w = src.getWidth();
            int h = src.getHeight();
            int max = Math.max(w, h);
            double scale = max > maxSide ? (double) maxSide / max : 1.0;
            if (scale >= 1.0 && data.length < 900_000) {
                return data;
            }
            int nw = Math.max(1, (int) Math.round(w * scale));
            int nh = Math.max(1, (int) Math.round(h * scale));
            BufferedImage scaled = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, nw, nh, null);
            g.dispose();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (!writers.hasNext()) {
                return data;
            }
            ImageWriter writer = writers.next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.82f);
            }
            ImageOutputStream ios = ImageIO.createImageOutputStream(bos);
            writer.setOutput(ios);
            writer.write(null, new IIOImage(scaled, null, null), param);
            writer.dispose();
            ios.close();
            byte[] out = bos.toByteArray();
            return out.length > 0 ? out : data;
        } catch (Exception e) {
            return data;
        }
    }

    private static String httpErrorDetail(String respBody) {
        if (respBody == null || respBody.isBlank()) {
            return null;
        }
        try {
            JsonObject o = JsonParser.parseString(respBody).getAsJsonObject();
            if (o.has("error")) {
                JsonElement errEl = o.get("error");
                if (errEl.isJsonObject()) {
                    JsonObject err = errEl.getAsJsonObject();
                    if (err.has("message")) {
                        return err.get("message").getAsString();
                    }
                } else if (errEl.isJsonPrimitive()) {
                    return errEl.getAsString();
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        return null;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private static String extractAssistantText(String responseJson) {
        JsonObject root = JsonParser.parseString(responseJson).getAsJsonObject();
        JsonArray choices = root.getAsJsonArray("choices");
        if (choices == null || choices.size() == 0) {
            if (root.has("error")) {
                JsonObject err = root.getAsJsonObject("error");
                String msg = err.has("message") ? err.get("message").getAsString() : err.toString();
                throw new AIEngineException("AI error: " + msg, null);
            }
            throw new AIEngineException("AI returned no choices.", null);
        }
        JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
        JsonElement contentEl = message.get("content");
        String fromContent = null;
        if (contentEl != null && !contentEl.isJsonNull()) {
            if (contentEl.isJsonPrimitive()) {
                fromContent = contentEl.getAsString();
            } else if (contentEl.isJsonArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonElement e : contentEl.getAsJsonArray()) {
                    if (e.isJsonObject()) {
                        JsonObject part = e.getAsJsonObject();
                        if (part.has("text")) {
                            sb.append(part.get("text").getAsString());
                        }
                    }
                }
                fromContent = sb.toString();
            } else {
                fromContent = contentEl.toString();
            }
        }
        if (fromContent != null && !fromContent.isBlank()) {
            return fromContent;
        }
        // Cerebras gpt-oss sometimes exposes chain-of-thought in "reasoning" when content is empty
        if (message.has("reasoning") && !message.get("reasoning").isJsonNull()) {
            JsonElement r = message.get("reasoning");
            if (r.isJsonPrimitive()) {
                String reasoning = r.getAsString();
                if (!reasoning.isBlank()) {
                    return reasoning;
                }
            }
        }
        throw new AIEngineException(
                "AI returned empty message content. Raw (truncated): " + truncate(responseJson, 400),
                null);
    }

    private static String extractJsonFromReply(String assistantContent) {
        if (assistantContent == null) {
            return "";
        }
        String c = assistantContent.trim();
        if (c.startsWith("```")) {
            int firstNl = c.indexOf('\n');
            int lastFence = c.lastIndexOf("```");
            if (firstNl > 0 && lastFence > firstNl) {
                c = c.substring(firstNl + 1, lastFence).trim();
                if (c.toLowerCase().startsWith("json")) {
                    int nl = c.indexOf('\n');
                    if (nl > 0) {
                        c = c.substring(nl + 1).trim();
                    }
                }
            }
        }
        int obj = c.indexOf('{');
        int arr = c.indexOf('[');
        int start = -1;
        if (obj >= 0 && (arr < 0 || obj < arr)) {
            start = obj;
        } else if (arr >= 0) {
            start = arr;
        }
        if (start > 0) {
            c = c.substring(start);
        }
        return c.trim();
    }

    private UMLModel parseUmlModelFromJson(String assistantContent) {
        String json = extractJsonFromReply(assistantContent);
        if (json.isBlank()) {
            throw new AIEngineException("AI returned no JSON. Raw: " + truncate(assistantContent, 200), null);
        }
        UMLModel model = new UMLModel();
        model.setRawJson(json);
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray classes = root.has("classes") ? root.getAsJsonArray("classes") : new JsonArray();
            Map<String, ConceptualClass> byName = new HashMap<>();

            for (int i = 0; i < classes.size(); i++) {
                JsonObject cObj = classes.get(i).getAsJsonObject();
                ConceptualClass c = new ConceptualClass();
                c.setClassId(UUID.randomUUID());
                c.setName(cObj.has("name") ? cObj.get("name").getAsString() : "Class" + i);
                c.setClassType(ClassType.ENTITY);
                if (cObj.has("isInterface") && cObj.get("isInterface").getAsBoolean()) {
                    c.setClassType(ClassType.INTERFACE);
                }
                if (cObj.has("isAbstract") && cObj.get("isAbstract").getAsBoolean()) {
                    c.setClassType(ClassType.ABSTRACT);
                }
                if (cObj.has("x")) {
                    c.setPositionX(cObj.get("x").getAsDouble());
                } else {
                    c.setPositionX(100 + (i % 4) * 240);
                }
                if (cObj.has("y")) {
                    c.setPositionY(cObj.get("y").getAsDouble());
                } else {
                    c.setPositionY(100 + (i / 4) * 200);
                }

                if (cObj.has("attributes")) {
                    JsonArray attrs = cObj.getAsJsonArray("attributes");
                    for (int j = 0; j < attrs.size(); j++) {
                        JsonObject aObj = attrs.get(j).getAsJsonObject();
                        Attribute a = new Attribute();
                        a.setAttributeId(UUID.randomUUID());
                        String an = aObj.has("attributeName") ? aObj.get("attributeName").getAsString()
                                : aObj.has("name") ? aObj.get("name").getAsString() : "field" + j;
                        a.setAttributeName(an);
                        a.setDataType(aObj.has("dataType") ? aObj.get("dataType").getAsString()
                                : aObj.has("type") ? aObj.get("type").getAsString() : "Object");
                        a.setVisibility(parseVisibility(aObj, Visibility.PRIVATE));
                        c.addAttribute(a);
                    }
                }
                if (cObj.has("methods")) {
                    JsonArray methods = cObj.getAsJsonArray("methods");
                    for (int j = 0; j < methods.size(); j++) {
                        JsonObject mObj = methods.get(j).getAsJsonObject();
                        Method mth = new Method();
                        mth.setMethodId(UUID.randomUUID());
                        mth.setMethodName(mObj.has("methodName") ? mObj.get("methodName").getAsString()
                                : mObj.has("name") ? mObj.get("name").getAsString() : "method" + j);
                        mth.setReturnType(mObj.has("returnType") ? mObj.get("returnType").getAsString() : "void");
                        if (mObj.has("parameters")) {
                            JsonElement pe = mObj.get("parameters");
                            mth.setParameters(pe.isJsonArray() ? pe.toString() : pe.getAsString());
                        } else {
                            mth.setParameters("");
                        }
                        mth.setVisibility(parseVisibility(mObj, Visibility.PUBLIC));
                        c.addMethod(mth);
                    }
                }
                model.getClasses().add(c);
                byName.put(c.getName(), c);
            }

            JsonArray rels = root.has("relationships") ? root.getAsJsonArray("relationships") : new JsonArray();
            for (int i = 0; i < rels.size(); i++) {
                JsonObject rObj = rels.get(i).getAsJsonObject();
                String srcName = readRelName(rObj, "sourceClass", "source");
                String tgtName = readRelName(rObj, "targetClass", "target");
                if (srcName == null || tgtName == null) {
                    continue;
                }
                ConceptualClass s = byName.get(srcName);
                ConceptualClass t = byName.get(tgtName);
                if (s == null || t == null) {
                    continue;
                }
                String typeStr = rObj.has("type") ? rObj.get("type").getAsString() : "ASSOCIATION";
                RelationshipType rt = parseRelationshipType(typeStr);
                Relationship r = buildRelationship(rt);
                r.setRelationshipId(UUID.randomUUID());
                r.setSourceClass(s);
                r.setTargetClass(t);
                if (rObj.has("sourceMultiplicity")) {
                    r.setSourceMultiplicity(rObj.get("sourceMultiplicity").getAsString());
                }
                if (rObj.has("targetMultiplicity")) {
                    r.setTargetMultiplicity(rObj.get("targetMultiplicity").getAsString());
                }
                if (rObj.has("label") && !rObj.get("label").isJsonNull()) {
                    r.setLabel(rObj.get("label").getAsString());
                }
                model.getRelationships().add(r);
            }

            return model;
        } catch (AIEngineException e) {
            throw e;
        } catch (Exception e) {
            throw new AIEngineException("Failed to parse AI JSON: " + e.getMessage() + ". Snippet: " + truncate(json, 180), e);
        }
    }

    private static String readRelName(JsonObject rObj, String a, String b) {
        if (rObj.has(a) && !rObj.get(a).isJsonNull()) {
            return rObj.get(a).getAsString();
        }
        if (rObj.has(b) && !rObj.get(b).isJsonNull()) {
            return rObj.get(b).getAsString();
        }
        return null;
    }

    private static RelationshipType parseRelationshipType(String typeStr) {
        if (typeStr == null || typeStr.isBlank()) {
            return RelationshipType.ASSOCIATION;
        }
        try {
            return RelationshipType.valueOf(typeStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return RelationshipType.ASSOCIATION;
        }
    }

    private static Relationship buildRelationship(RelationshipType rt) {
        switch (rt) {
            case COMPOSITION: {
                AssociationRelationship a = new AssociationRelationship();
                a.setComposition(true);
                return a;
            }
            case AGGREGATION: {
                AssociationRelationship a = new AssociationRelationship();
                a.setAggregation(true);
                return a;
            }
            case ASSOCIATION:
                return new AssociationRelationship();
            case INHERITANCE: {
                InheritanceRelationship i = new InheritanceRelationship();
                i.setInterface(false);
                return i;
            }
            case REALIZATION: {
                InheritanceRelationship i = new InheritanceRelationship();
                i.setInterface(true);
                i.setDashed(true);
                return i;
            }
            case DEPENDENCY: {
                DependencyRelationship d = new DependencyRelationship();
                d.setDashed(true);
                return d;
            }
            default:
                return new AssociationRelationship();
        }
    }

    private static Visibility parseVisibility(JsonObject o, Visibility def) {
        if (!o.has("visibility") || o.get("visibility").isJsonNull()) {
            return def;
        }
        try {
            return Visibility.valueOf(o.get("visibility").getAsString().trim().toUpperCase());
        } catch (Exception e) {
            return def;
        }
    }

    private DesignEvaluationReport parseEvaluationJson(String assistantContent, UMLModel diagramModel) {
        DesignEvaluationReport report = new DesignEvaluationReport();
        String json = extractJsonFromReply(assistantContent);
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            root = coalesceEvaluationJsonRoot(root);
            Float c = readScoreFlexible(root, "couplingScore", "coupling", "coupling_score");
            Float co = readScoreFlexible(root, "cohesionScore", "cohesion", "cohesion_score");
            Float s = readScoreFlexible(root, "solidScore", "solid", "solid_score", "solidPrinciplesScore");

            report.setCouplingScore(normalizeScoreOrDefault(c, 6.5f));
            report.setCohesionScore(normalizeScoreOrDefault(co, 6.5f));
            report.setSolidScore(normalizeScoreOrDefault(s, 6.5f));

            if (root.has("summary") && !root.get("summary").isJsonNull()) {
                report.setFeedbackSummary(root.get("summary").getAsString());
            } else if (root.has("feedbackSummary")) {
                report.setFeedbackSummary(root.get("feedbackSummary").getAsString());
            }

            if (root.has("suggestions") && root.get("suggestions").isJsonArray()) {
                JsonArray arr = root.getAsJsonArray("suggestions");
                java.util.List<String> list = new java.util.ArrayList<>();
                for (JsonElement e : arr) {
                    if (e.isJsonPrimitive()) {
                        list.add(e.getAsString());
                    } else if (e.isJsonObject() && e.getAsJsonObject().has("text")) {
                        list.add(e.getAsJsonObject().get("text").getAsString());
                    }
                }
                report.setSuggestions(list);
            }

            if (allScoresSuspicious(report)) {
                applyScoresFromRegex(assistantContent, report);
            }
        } catch (Exception e) {
            report.setFeedbackSummary(assistantContent != null ? truncate(assistantContent, 800) : "Evaluation parse failed.");
            regexScoresOnly(assistantContent, report);
            if (report.getCouplingScore() == 0 && report.getCohesionScore() == 0 && report.getSolidScore() == 0) {
                report.setCouplingScore(6f);
                report.setCohesionScore(6f);
                report.setSolidScore(6f);
            }
        }
        if (diagramModel != null) {
            validateEvaluationAgainstDiagram(report, diagramModel);
        }
        return report;
    }

    private static JsonObject coalesceEvaluationJsonRoot(JsonObject root) {
        if (root == null) {
            return new JsonObject();
        }
        if (root.has("couplingScore") || root.has("cohesionScore") || root.has("solidScore")
                || root.has("summary") || root.has("feedbackSummary") || root.has("suggestions")) {
            return root;
        }
        if (root.has("properties") && root.get("properties").isJsonObject()) {
            JsonObject p = root.getAsJsonObject("properties");
            if (p.has("couplingScore") || p.has("summary") || p.has("suggestions") || p.has("cohesionScore")) {
                return p;
            }
        }
        return root;
    }

    private static boolean mentionsAnyDiagramClass(String text, java.util.List<String> classNames) {
        if (text == null || text.isBlank() || classNames == null || classNames.isEmpty()) {
            return false;
        }
        for (String name : classNames) {
            if (name == null || name.isBlank()) {
                continue;
            }
            if (name.length() == 1) {
                if (text.contains(name)) {
                    return true;
                }
                continue;
            }
            if (Pattern.compile("\\b" + Pattern.quote(name) + "\\b").matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    private static void validateEvaluationAgainstDiagram(DesignEvaluationReport report, UMLModel m) {
        java.util.List<String> names = AiDiagramPayload.classNames(m);
        if (names.isEmpty()) {
            return;
        }
        String summary = report.getFeedbackSummary();
        java.util.List<String> sug = new java.util.ArrayList<>(report.getSuggestions());
        boolean ok = mentionsAnyDiagramClass(summary, names);
        if (!ok) {
            for (String s : sug) {
                if (mentionsAnyDiagramClass(s, names)) {
                    ok = true;
                    break;
                }
            }
        }
        if (!ok) {
            report.setSuggestions(java.util.List.of());
            report.setFeedbackSummary(
                    "The AI reply did not reference any class from your diagram ("
                            + String.join(", ", names)
                            + "). Try Evaluate again or switch model.\n\n"
                            + (summary != null && !summary.isBlank()
                                    ? "— Model text (for debugging) —\n" + summary
                                    : ""));
            return;
        }
        java.util.List<String> filtered = new java.util.ArrayList<>();
        for (String s : sug) {
            if (mentionsAnyDiagramClass(s, names)) {
                filtered.add(s);
            }
        }
        report.setSuggestions(filtered);
    }

    private static boolean allScoresSuspicious(DesignEvaluationReport r) {
        return r.getCouplingScore() <= 0.01f && r.getCohesionScore() <= 0.01f && r.getSolidScore() <= 0.01f;
    }

    private static Float readScoreFlexible(JsonObject root, String... keys) {
        for (String key : keys) {
            if (root.has(key) && !root.get(key).isJsonNull()) {
                Float v = parseFloatElement(root.get(key));
                if (v != null) {
                    return v;
                }
            }
        }
        if (root.has("scores") && root.get("scores").isJsonObject()) {
            JsonObject scores = root.getAsJsonObject("scores");
            for (String key : keys) {
                if (scores.has(key) && !scores.get(key).isJsonNull()) {
                    Float v = parseFloatElement(scores.get(key));
                    if (v != null) {
                        return v;
                    }
                }
            }
        }
        return null;
    }

    private static Float parseFloatElement(JsonElement el) {
        try {
            if (el.isJsonPrimitive()) {
                if (el.getAsJsonPrimitive().isNumber()) {
                    return el.getAsFloat();
                }
                if (el.getAsJsonPrimitive().isString()) {
                    return Float.parseFloat(el.getAsString().trim());
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private static float normalizeScoreOrDefault(Float raw, float def) {
        if (raw == null) {
            return def;
        }
        float v = raw;
        if (v > 10f && v <= 100f) {
            v = v / 10f;
        }
        if (v <= 0f || Float.isNaN(v)) {
            return def;
        }
        return Math.min(10f, Math.max(1f, v));
    }

    private static void applyScoresFromRegex(String text, DesignEvaluationReport report) {
        if (text == null || text.isBlank()) {
            return;
        }
        Float c = matchOneScore(text, Pattern.compile("\"couplingScore\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE));
        Float co = matchOneScore(text, Pattern.compile("\"cohesionScore\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE));
        Float so = matchOneScore(text, Pattern.compile("\"solidScore\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE));
        if (c != null) {
            report.setCouplingScore(normalizeScoreOrDefault(c, report.getCouplingScore()));
        }
        if (co != null) {
            report.setCohesionScore(normalizeScoreOrDefault(co, report.getCohesionScore()));
        }
        if (so != null) {
            report.setSolidScore(normalizeScoreOrDefault(so, report.getSolidScore()));
        }
    }

    private static void regexScoresOnly(String text, DesignEvaluationReport report) {
        applyScoresFromRegex(text, report);
    }

    private static Float matchOneScore(String text, Pattern p) {
        Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                return Float.parseFloat(m.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static String detectImageMime(byte[] data) {
        if (data.length >= 3 && data[0] == (byte) 0xFF && data[1] == (byte) 0xD8) {
            return "image/jpeg";
        }
        if (data.length >= 8 && data[0] == (byte) 0x89 && data[1] == 0x50) {
            return "image/png";
        }
        return "image/png";
    }

    private static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
