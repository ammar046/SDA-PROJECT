package com.umlytics.services;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Normalizes LLM skeleton JSON (OpenAI / Gemini JSON-mode quirks, nested "properties", bad encodings).
 */
public final class SkeletonResponseNormalizer {

    private SkeletonResponseNormalizer() {
    }

    public static String toCleanSkeletonsJson(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return "{\"skeletons\":{}}";
        }
        String trimmed = rawResponse.trim();
        if (!trimmed.startsWith("{")) {
            return "{\"skeletons\":{\"Generated\":\"" + escape(trimmed) + "\"}}";
        }
        try {
            JsonObject root = JsonParser.parseString(trimmed).getAsJsonObject();
            JsonObject sk = extractSkeletonsMap(root);
            if (sk == null) {
                return trimmed;
            }
            JsonObject out = new JsonObject();
            JsonObject outSk = new JsonObject();
            for (String key : sk.keySet()) {
                String java = valueToJavaSource(sk.get(key));
                if (java != null && !java.isBlank()) {
                    outSk.addProperty(key, java);
                }
            }
            out.add("skeletons", outSk);
            return out.toString();
        } catch (Exception e) {
            return trimmed;
        }
    }

    private static JsonObject extractSkeletonsMap(JsonObject root) {
        if (root.has("skeletons")) {
            JsonElement el = root.get("skeletons");
            if (el.isJsonObject()) {
                return el.getAsJsonObject();
            }
        }
        if (root.has("properties") && root.get("properties").isJsonObject()) {
            JsonObject props = root.getAsJsonObject("properties");
            if (props.has("skeletons") && props.get("skeletons").isJsonObject()) {
                JsonObject sk = props.getAsJsonObject("skeletons");
                if (sk.has("properties") && sk.get("properties").isJsonObject()) {
                    return sk.getAsJsonObject("properties");
                }
                return sk;
            }
        }
        if (root.has("type") && root.has("properties") && root.get("properties").isJsonObject()) {
            JsonObject props = root.getAsJsonObject("properties");
            if (props.has("skeletons") && props.get("skeletons").isJsonObject()) {
                JsonObject sk = props.getAsJsonObject("skeletons");
                if (sk.has("properties") && sk.get("properties").isJsonObject()) {
                    return sk.getAsJsonObject("properties");
                }
                return sk;
            }
        }
        return null;
    }

    private static String valueToJavaSource(JsonElement v) {
        if (v == null || v.isJsonNull()) {
            return null;
        }
        if (v.isJsonPrimitive()) {
            JsonPrimitive p = v.getAsJsonPrimitive();
            if (p.isString()) {
                return sanitizeJavaString(p.getAsString());
            }
            return p.getAsString();
        }
        if (v.isJsonObject()) {
            JsonObject o = v.getAsJsonObject();
            if (o.has("source") && o.get("source").isJsonPrimitive()) {
                return sanitizeJavaString(o.get("source").getAsString());
            }
            if (o.has("code") && o.get("code").isJsonPrimitive()) {
                return sanitizeJavaString(o.get("code").getAsString());
            }
            return null;
        }
        return null;
    }

    /**
     * Rejects pseudo-Java encoded as JSON objects ({ "package": "com...", "import": "..." }).
     * Accepts real Java: package, import, comment, or modifier-starting type.
     */
    static String sanitizeJavaString(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.startsWith("{") && t.contains("\"package\"") && t.contains(":")) {
            return null;
        }
        char c0 = t.charAt(0);
        boolean plausible = t.startsWith("package ") || t.startsWith("import ") || c0 == '/' || t.startsWith("public ")
                || t.startsWith("private ") || t.startsWith("protected ") || t.startsWith("class ")
                || t.startsWith("interface ") || t.startsWith("enum ") || t.startsWith("@")
                || t.startsWith("abstract ");
        if (!plausible && t.startsWith("{")) {
            return null;
        }
        return s;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    /** Keep only skeleton entries whose key is in allowed (if allowed non-empty). */
    public static String filterSkeletonKeys(String skeletonsJson, Set<String> allowedClassNames) {
        if (skeletonsJson == null || allowedClassNames == null || allowedClassNames.isEmpty()) {
            return skeletonsJson;
        }
        try {
            JsonObject root = JsonParser.parseString(skeletonsJson).getAsJsonObject();
            if (!root.has("skeletons") || !root.get("skeletons").isJsonObject()) {
                return skeletonsJson;
            }
            JsonObject sk = root.getAsJsonObject("skeletons");
            JsonObject filtered = new JsonObject();
            for (String key : sk.keySet()) {
                if (allowedClassNames.contains(key)) {
                    filtered.add(key, sk.get(key));
                }
            }
            JsonObject out = new JsonObject();
            out.add("skeletons", filtered);
            return out.toString();
        } catch (Exception e) {
            return skeletonsJson;
        }
    }

    public static Map<String, String> skeletonsToMap(String skeletonsJson) {
        Map<String, String> map = new LinkedHashMap<>();
        if (skeletonsJson == null || skeletonsJson.isBlank()) {
            return map;
        }
        try {
            JsonObject root = JsonParser.parseString(skeletonsJson.trim()).getAsJsonObject();
            if (!root.has("skeletons") || !root.get("skeletons").isJsonObject()) {
                return map;
            }
            JsonObject sk = root.getAsJsonObject("skeletons");
            for (String key : sk.keySet()) {
                String java = valueToJavaSource(sk.get(key));
                if (java != null && !java.isBlank()) {
                    map.put(key, java);
                }
            }
        } catch (Exception ignored) {
            // leave empty
        }
        return map;
    }
}
