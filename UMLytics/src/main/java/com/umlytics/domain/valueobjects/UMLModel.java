package com.umlytics.domain.valueobjects;

import com.umlytics.domain.UMLClass;
import com.umlytics.domain.relationships.Relationship;
import java.util.ArrayList;
import java.util.List;

/**
 * Transient UML model returned by AI/Parser; converts to UMLDiagram.
 */
public class UMLModel {
    private List<UMLClass>    classes       = new ArrayList<>();
    private List<Relationship> relationships = new ArrayList<>();
    private String             rawJson;

    public UMLModel() {}

    public UMLModel(List<UMLClass> classes, List<Relationship> relationships, String rawJson) {
        this.classes       = classes != null ? classes : new ArrayList<>();
        this.relationships = relationships != null ? relationships : new ArrayList<>();
        this.rawJson       = rawJson;
    }

    public com.umlytics.domain.UMLDiagram toUMLDiagram() {
        com.umlytics.domain.UMLDiagram diagram = new com.umlytics.domain.UMLDiagram();
        for (UMLClass c : classes)       diagram.addUMLClass(c);
        for (Relationship r : relationships) diagram.addRelationship(r);
        return diagram;
    }

    public boolean validate() {
        return classes != null && !classes.isEmpty();
    }

    public List<UMLClass>     getClasses()            { return classes; }
    public void               setClasses(List<UMLClass> c) { this.classes = c; }
    public List<Relationship> getRelationships()       { return relationships; }
    public void               setRelationships(List<Relationship> r) { this.relationships = r; }
    public String             getRawJson()             { return rawJson; }
    public void               setRawJson(String j)    { this.rawJson = j; }
}
