package com.umlytics.util;

import com.umlytics.domain.UMLClass;
import com.umlytics.domain.relationships.*;
import com.umlytics.enums.Navigability;
import com.umlytics.enums.RelationshipType;

/**
 * Utility for creating specific Relationship objects safely.
 * GoF: Factory Method
 */
public class RelationshipFactory {

    public static Relationship createRelationship(RelationshipType type, UMLClass source, UMLClass target) {
        if (source == null || target == null) return null;

        return switch (type) {
            case INHERITANCE -> new InheritanceRelationship(0, source, target, false);
            case REALIZATION -> new InheritanceRelationship(0, source, target, true);
            case COMPOSITION -> new AssociationRelationship(0, source, target, true, false, Navigability.UNIDIRECTIONAL);
            case AGGREGATION -> new AssociationRelationship(0, source, target, false, true, Navigability.UNIDIRECTIONAL);
            case DEPENDENCY  -> new DependencyRelationship(0, source, target, "uses");
            case ASSOCIATION -> new AssociationRelationship(0, source, target, false, false, Navigability.UNIDIRECTIONAL);
        };
    }
}
