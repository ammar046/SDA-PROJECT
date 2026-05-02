package com.umlytics.domain;

import com.umlytics.enums.RelationshipType;

public abstract class Relationship {
    private int relationshipId;
    private UMLClass sourceClass;
    private UMLClass targetClass;
    private String sourceMultiplicity;
    private String targetMultiplicity;
    private String label;
    private Double bendX;
    private String edgeColor = "Black";
    private boolean dashed;

    public abstract RelationshipType getType();

    public UMLClass getSource() {
        return sourceClass;
    }

    public UMLClass getTarget() {
        return targetClass;
    }

    public String getSourceMultiplicity() {
        return sourceMultiplicity;
    }

    public String getTargetMultiplicity() {
        return targetMultiplicity;
    }

    public boolean validate() {
        return sourceClass != null && targetClass != null && sourceClass != targetClass;
    }

    public int getRelationshipId() {
        return relationshipId;
    }

    public void setRelationshipId(int relationshipId) {
        this.relationshipId = relationshipId;
    }

    public void setSourceClass(UMLClass sourceClass) {
        this.sourceClass = sourceClass;
    }

    public void setTargetClass(UMLClass targetClass) {
        this.targetClass = targetClass;
    }

    public void setSourceMultiplicity(String sourceMultiplicity) {
        this.sourceMultiplicity = sourceMultiplicity;
    }

    public void setTargetMultiplicity(String targetMultiplicity) {
        this.targetMultiplicity = targetMultiplicity;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Double getBendX() {
        return bendX;
    }

    public void setBendX(Double bendX) {
        this.bendX = bendX;
    }

    public String getEdgeColor() {
        return edgeColor;
    }

    public void setEdgeColor(String edgeColor) {
        this.edgeColor = edgeColor;
    }

    public boolean isDashed() {
        return dashed;
    }

    public void setDashed(boolean dashed) {
        this.dashed = dashed;
    }
}
