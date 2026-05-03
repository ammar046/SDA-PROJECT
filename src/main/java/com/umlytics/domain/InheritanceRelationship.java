package com.umlytics.domain;

import com.umlytics.enums.RelationshipType;

public class InheritanceRelationship extends Relationship {
    private boolean isInterface;

    @Override
    public RelationshipType getType() {
        return isInterface ? RelationshipType.REALIZATION : RelationshipType.INHERITANCE;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public void setInterface(boolean anInterface) {
        isInterface = anInterface;
    }
}
