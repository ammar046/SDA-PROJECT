package com.umlytics.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.umlytics.domain.Attribute;
import com.umlytics.domain.ConceptualClass;
import com.umlytics.domain.DesignEvaluationReport;
import com.umlytics.domain.Method;
import com.umlytics.domain.ProjectContext;
import com.umlytics.domain.UMLModel;
import com.umlytics.enums.ClassType;
import com.umlytics.enums.Visibility;
import com.umlytics.exceptions.AIEngineException;
import com.umlytics.interfaces.IAIEngine;

import java.util.UUID;

// GRASP: Pure Fabrication
public class LLMAPIEngine implements IAIEngine {
    @Override
    public UMLModel generateFromText(String desc) {
        if (desc == null || desc.isBlank()) {
            throw new AIEngineException("Description cannot be empty.", null);
        }
        UMLModel model = new UMLModel();
        model.setRawJson("{\"source\":\"offline\",\"description\":\"" + desc.replace("\"", "\\\"") + "\"}");
        return model;
    }

    @Override
    public DesignEvaluationReport evaluateDesign(UMLModel m) {
        DesignEvaluationReport report = new DesignEvaluationReport();
        report.setCouplingScore(6.0f);
        report.setCohesionScore(6.5f);
        report.setSolidScore(6.0f);
        report.setFeedbackSummary("Offline analysis completed.");
        return report;
    }

    @Override
    public String consultDesign(String q, ProjectContext ctx) {
        return "Offline response: " + (q == null ? "" : q.trim());
    }

    @Override
    public String generateStructure(UMLModel m) {
        return "{\"skeletons\":{\"Generated\":\"public class Generated {}\"}}";
    }

    @Override
    public UMLModel analyzeImage(byte[] data) {
        if (data == null || data.length == 0) {
            throw new AIEngineException("Uploaded image data is empty.", null);
        }
        UMLModel model = new UMLModel();
        model.setRawJson("{\"source\":\"image\",\"bytes\":" + data.length + "}");
        return model;
    }

    @SuppressWarnings("unused")
    private UMLModel parseResponse(String json) {
        UMLModel model = new UMLModel();
        if (json == null || json.isBlank()) {
            return model;
        }
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray classes = root.has("classes") ? root.getAsJsonArray("classes") : new JsonArray();
        for (int i = 0; i < classes.size(); i++) {
            JsonObject cObj = classes.get(i).getAsJsonObject();
            ConceptualClass c = new ConceptualClass();
            c.setClassId(UUID.randomUUID());
            c.setName(cObj.has("name") ? cObj.get("name").getAsString() : "Class" + i);
            c.setClassType(ClassType.ENTITY);
            if (cObj.has("isInterface") && cObj.get("isInterface").getAsBoolean()) c.setClassType(ClassType.INTERFACE);
            if (cObj.has("isAbstract") && cObj.get("isAbstract").getAsBoolean()) c.setClassType(ClassType.ABSTRACT);
            if (cObj.has("attributes")) {
                JsonArray attrs = cObj.getAsJsonArray("attributes");
                for (int j = 0; j < attrs.size(); j++) {
                    JsonObject aObj = attrs.get(j).getAsJsonObject();
                    Attribute a = new Attribute();
                    a.setAttributeId(UUID.randomUUID());
                    a.setAttributeName(aObj.has("attributeName") ? aObj.get("attributeName").getAsString() : aObj.has("name") ? aObj.get("name").getAsString() : "field");
                    a.setDataType(aObj.has("dataType") ? aObj.get("dataType").getAsString() : aObj.has("type") ? aObj.get("type").getAsString() : "Object");
                    a.setVisibility(Visibility.PRIVATE);
                    c.addAttribute(a);
                }
            }
            if (cObj.has("methods")) {
                JsonArray methods = cObj.getAsJsonArray("methods");
                for (int j = 0; j < methods.size(); j++) {
                    JsonObject mObj = methods.get(j).getAsJsonObject();
                    Method mth = new Method();
                    mth.setMethodId(UUID.randomUUID());
                    mth.setMethodName(mObj.has("methodName") ? mObj.get("methodName").getAsString() : mObj.has("name") ? mObj.get("name").getAsString() : "method");
                    mth.setReturnType(mObj.has("returnType") ? mObj.get("returnType").getAsString() : "void");
                    mth.setParameters(mObj.has("parameters") ? mObj.get("parameters").toString() : "");
                    mth.setVisibility(Visibility.PUBLIC);
                    c.addMethod(mth);
                }
            }
            model.getClasses().add(c);
        }
        return model;
    }
}
