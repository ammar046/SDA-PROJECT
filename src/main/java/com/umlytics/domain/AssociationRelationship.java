package com.umlytics.domain;

import com.umlytics.enums.Navigability;
import com.umlytics.enums.RelationshipType;

public class AssociationRelationship extends Relationship {
    private boolean isComposition;
    private boolean isAggregation;
    private Navigability navigability;

    @Override
    public RelationshipType getType() {
        if (isComposition) {
            return RelationshipType.COMPOSITION;
        }
        if (isAggregation) {
            return RelationshipType.AGGREGATION;
        }
        return RelationshipType.ASSOCIATION;
    }

    public boolean isComposition() {
        return isComposition;
    }

    public boolean isAggregation() {
        return isAggregation;
    }

    public Navigability getNavigability() {
        return navigability;
    }

    public void setComposition(boolean composition) {
        isComposition = composition;
    }

    public void setAggregation(boolean aggregation) {
        isAggregation = aggregation;
    }

    public void setNavigability(Navigability navigability) {
        this.navigability = navigability;
    }
}
