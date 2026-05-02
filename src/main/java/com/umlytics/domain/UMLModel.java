package com.umlytics.domain;

import java.util.ArrayList;
import java.util.List;

public class UMLModel {
    private List<UMLClass> classes = new ArrayList<>();
    private List<Relationship> relationships = new ArrayList<>();
    private String rawJson;

    public UMLDiagram toUMLDiagram() {
        UMLDiagram diagram = new UMLDiagram();
        for (UMLClass umlClass : classes) {
            diagram.addUMLClass(umlClass);
        }
        for (Relationship relationship : relationships) {
            diagram.addRelationship(relationship);
        }
        return diagram;
    }

    public boolean validate() {
        return classes != null && relationships != null;
    }

    public List<UMLClass> getClasses() {
        return classes;
    }

    public void setClasses(List<UMLClass> classes) {
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
