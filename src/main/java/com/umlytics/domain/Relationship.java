package com.umlytics.domain;

import com.umlytics.enums.RelationshipType;
import java.util.UUID;

public abstract class Relationship {
    private UUID relationshipId;
    private ConceptualClass sourceClass;
    private ConceptualClass targetClass;
    private String sourceMultiplicity;
    private String targetMultiplicity;
    private String label;
    private Double bendX;
    private String edgeColor = "Black";
    private boolean dashed;

    public abstract RelationshipType getType();

    public ConceptualClass getSource() {
        return sourceClass;
    }

    public ConceptualClass getTarget() {
        return targetClass;
    }

    public String getSourceMultiplicity() {
        return sourceMultiplicity;
    }

    public String getTargetMultiplicity() {
        return targetMultiplicity;
    }

    public boolean validate() {
        return sourceClass != null
                && targetClass != null
                && sourceClass.getClassId() != null
                && targetClass.getClassId() != null
                && !sourceClass.getClassId().equals(targetClass.getClassId());
    }

    public UUID getRelationshipId() {
        return relationshipId;
    }

    public void setRelationshipId(UUID relationshipId) {
        this.relationshipId = relationshipId;
    }

    public void setSourceClass(ConceptualClass sourceClass) {
        this.sourceClass = sourceClass;
    }

    public void setTargetClass(ConceptualClass targetClass) {
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
