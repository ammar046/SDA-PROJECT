package com.umlytics.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.umlytics.domain.Attribute;
import com.umlytics.domain.ConceptualClass;
import com.umlytics.domain.Method;
import com.umlytics.domain.Relationship;
import com.umlytics.domain.UMLDiagram;
import com.umlytics.domain.UMLModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Rich JSON for AI prompts (classes with members and relationships). {@link UMLDiagram#serialize()} is
 * layout-oriented and omits attributes/methods — do not use it alone for evaluation or code generation.
 */
public final class AiDiagramPayload {

    private AiDiagramPayload() {
    }

    public static String evaluationPayload(UMLDiagram diagram) {
        if (diagram == null) {
            return "{}";
        }
        UMLModel m = new UMLModel();
        m.setClasses(new ArrayList<>(diagram.getClasses()));
        m.setRelationships(new ArrayList<>(diagram.getRelationships()));
        JsonObject root = new JsonObject();
        root.addProperty("diagramTitle", diagram.getTitle() != null ? diagram.getTitle() : "");
        if (diagram.getDiagramId() != null) {
            root.addProperty("diagramId", diagram.getDiagramId().toString());
        }
        root.add("model", JsonParser.parseString(summarizeModelAsJson(m)).getAsJsonObject());
        return root.toString();
    }

    public static String summarizeModelAsJson(UMLModel m) {
        JsonObject root = new JsonObject();
        JsonArray clsArr = new JsonArray();
        if (m.getClasses() != null) {
            for (ConceptualClass c : m.getClasses()) {
                JsonObject jc = new JsonObject();
                jc.addProperty("name", c.getName() != null ? c.getName() : "Unnamed");
                jc.addProperty("kind", classKindLabel(c));
                JsonArray attrs = new JsonArray();
                for (Attribute a : c.getAttributes()) {
                    JsonObject ja = new JsonObject();
                    ja.addProperty("name", a.getName() != null ? a.getName() : "field");
                    ja.addProperty("type", a.getType() != null ? a.getType() : "Object");
                    ja.addProperty("visibility", a.getVisibility() != null ? a.getVisibility().name() : "PRIVATE");
                    ja.addProperty("static", a.isStatic());
                    attrs.add(ja);
                }
                jc.add("attributes", attrs);
                JsonArray meths = new JsonArray();
                for (Method met : c.getMethods()) {
                    JsonObject jm = new JsonObject();
                    jm.addProperty("name", met.getName() != null ? met.getName() : "method");
                    jm.addProperty("returnType", met.getReturnType() != null ? met.getReturnType() : "void");
                    jm.addProperty("parameters", met.getParameters() != null ? met.getParameters() : "");
                    jm.addProperty("visibility", met.getVisibility() != null ? met.getVisibility().name() : "PUBLIC");
                    jm.addProperty("abstract", met.isAbstract());
                    meths.add(jm);
                }
                jc.add("methods", meths);
                clsArr.add(jc);
            }
        }
        root.add("classes", clsArr);
        JsonArray relArr = new JsonArray();
        if (m.getRelationships() != null) {
            for (Relationship r : m.getRelationships()) {
                if (r.getSource() == null || r.getTarget() == null) {
                    continue;
                }
                JsonObject jr = new JsonObject();
                jr.addProperty("from", r.getSource().getName());
                jr.addProperty("to", r.getTarget().getName());
                jr.addProperty("type", r.getType().name());
                if (r.getSourceMultiplicity() != null) {
                    jr.addProperty("sourceMultiplicity", r.getSourceMultiplicity());
                }
                if (r.getTargetMultiplicity() != null) {
                    jr.addProperty("targetMultiplicity", r.getTargetMultiplicity());
                }
                if (r.getLabel() != null && !r.getLabel().isBlank()) {
                    jr.addProperty("label", r.getLabel());
                }
                relArr.add(jr);
            }
        }
        root.add("relationships", relArr);
        return root.toString();
    }

    private static String classKindLabel(ConceptualClass c) {
        if (c.isInterface()) {
            return "interface";
        }
        if (c.isAbstract()) {
            return "abstract_class";
        }
        if (c.isEnum()) {
            return "enum";
        }
        return "class";
    }

    /** Ordered UML type names (classes / interfaces / enums) for prompts and validation. */
    public static List<String> classNames(UMLModel m) {
        if (m == null || m.getClasses() == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (ConceptualClass c : m.getClasses()) {
            if (c.getName() != null && !c.getName().isBlank()) {
                out.add(c.getName());
            }
        }
        return out;
    }

    public static Set<String> classNameSet(UMLModel m) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(classNames(m)));
    }

    public static String allowedNamesForPrompt(UMLModel m) {
        List<String> n = classNames(m);
        if (n.isEmpty()) {
            return "(no named classes)";
        }
        return String.join(", ", n);
    }
}
