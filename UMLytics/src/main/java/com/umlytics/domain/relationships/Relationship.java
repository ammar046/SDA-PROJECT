package com.umlytics.domain.relationships;

import com.umlytics.domain.UMLClass;
import com.umlytics.enums.RelationshipType;

/**
 * Abstract base for all UML relationships.
 * GRASP: Information Expert
 * GoF: Template Method (validate())
 */
public abstract class Relationship {
    private int      relationshipId;
    private UMLClass sourceClass;
    private UMLClass targetClass;
    private String   sourceMultiplicity;
    private String   targetMultiplicity;
    private String   label;

    protected Relationship() {}

    protected Relationship(int id, UMLClass source, UMLClass target) {
        this.relationshipId = id;
        this.sourceClass    = source;
        this.targetClass    = target;
    }

    public abstract RelationshipType getType();

    public boolean validate() {
        return sourceClass != null && targetClass != null;
    }

    public int      getRelationshipId()          { return relationshipId; }
    public void     setRelationshipId(int id)    { this.relationshipId = id; }
    public UMLClass getSourceClass()             { return sourceClass; }
    public void     setSourceClass(UMLClass c)   { this.sourceClass = c; }
    public UMLClass getTargetClass()             { return targetClass; }
    public void     setTargetClass(UMLClass c)   { this.targetClass = c; }
    public String   getSourceMultiplicity()      { return sourceMultiplicity; }
    public void     setSourceMultiplicity(String s) { this.sourceMultiplicity = s; }
    public String   getTargetMultiplicity()      { return targetMultiplicity; }
    public void     setTargetMultiplicity(String t) { this.targetMultiplicity = t; }
    public String   getLabel()                   { return label; }
    public void     setLabel(String l)           { this.label = l; }

    @Override
    public String toString() {
        String src = sourceClass != null ? sourceClass.getName() : "?";
        String tgt = targetClass != null ? targetClass.getName() : "?";
        return src + " --[" + getType() + "]--> " + tgt;
    }
}
