package com.umlytics.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UMLModel {
    private List<ConceptualClass> classes = new ArrayList<>();
    private List<Relationship> relationships = new ArrayList<>();
    private String rawJson;

    public UMLDiagram toUMLDiagram() {
        UMLDiagram diagram = new UMLDiagram();
        diagram.setDiagramId(UUID.randomUUID());
        for (ConceptualClass umlClass : classes) {
            diagram.addConceptualClass(umlClass);
        }
        for (Relationship relationship : relationships) {
            diagram.addRelationship(relationship);
        }
        return diagram;
    }

    public boolean validate() {
        return classes != null && relationships != null;
    }

    public List<ConceptualClass> getClasses() {
        return classes;
    }

    public void setClasses(List<ConceptualClass> classes) {
        this.classes = classes;
    }

    public List<Relationship> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<Relationship> relationships) {
        this.relationships = relationships;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }
}
