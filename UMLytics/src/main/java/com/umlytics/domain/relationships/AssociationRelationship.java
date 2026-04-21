package com.umlytics.domain.relationships;

import com.umlytics.domain.UMLClass;
import com.umlytics.enums.Navigability;
import com.umlytics.enums.RelationshipType;

/**
 * Represents Association, Composition, or Aggregation.
 */
public class AssociationRelationship extends Relationship {
    private boolean     isComposition;
    private boolean     isAggregation;
    private Navigability navigability;

    public AssociationRelationship() {}

    public AssociationRelationship(int id, UMLClass src, UMLClass tgt,
                                   boolean isComposition, boolean isAggregation,
                                   Navigability nav) {
        super(id, src, tgt);
        this.isComposition = isComposition;
        this.isAggregation = isAggregation;
        this.navigability  = nav;
    }

    @Override
    public RelationshipType getType() {
        if (isComposition) return RelationshipType.COMPOSITION;
        if (isAggregation) return RelationshipType.AGGREGATION;
        return RelationshipType.ASSOCIATION;
    }

    public boolean     isComposition()       { return isComposition; }
    public void        setComposition(boolean v) { this.isComposition = v; }
    public boolean     isAggregation()       { return isAggregation; }
    public void        setAggregation(boolean v) { this.isAggregation = v; }
    public Navigability getNavigability()    { return navigability; }
    public void        setNavigability(Navigability n) { this.navigability = n; }
}
