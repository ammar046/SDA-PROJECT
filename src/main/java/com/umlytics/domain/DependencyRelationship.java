package com.umlytics.domain;

import com.umlytics.enums.RelationshipType;

public class DependencyRelationship extends Relationship {
    private String dependencyType;

    @Override
    public RelationshipType getType() {
        return RelationshipType.DEPENDENCY;
    }

    public String getDependencyType() {
        return dependencyType;
    }

    public void setDependencyType(String dependencyType) {
        this.dependencyType = dependencyType;
    }
}
