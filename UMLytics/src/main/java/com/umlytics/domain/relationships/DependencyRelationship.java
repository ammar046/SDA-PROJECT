package com.umlytics.domain.relationships;

import com.umlytics.domain.UMLClass;
import com.umlytics.enums.RelationshipType;

/**
 * Represents a dependency (uses) relationship.
 */
public class DependencyRelationship extends Relationship {
    private String dependencyType;

    public DependencyRelationship() {}

    public DependencyRelationship(int id, UMLClass src, UMLClass tgt, String dependencyType) {
        super(id, src, tgt);
        this.dependencyType = dependencyType;
    }

    @Override
    public RelationshipType getType() { return RelationshipType.DEPENDENCY; }

    public String getDependencyType()         { return dependencyType; }
    public void   setDependencyType(String t) { this.dependencyType = t; }
}
