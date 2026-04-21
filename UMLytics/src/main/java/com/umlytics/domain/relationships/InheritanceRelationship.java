package com.umlytics.domain.relationships;

import com.umlytics.domain.UMLClass;
import com.umlytics.enums.RelationshipType;

/**
 * Represents extends or implements relationship.
 */
public class InheritanceRelationship extends Relationship {
    private boolean isInterface; // true = realization (implements)

    public InheritanceRelationship() {}

    public InheritanceRelationship(int id, UMLClass source, UMLClass target, boolean isInterface) {
        super(id, source, target);
        this.isInterface = isInterface;
    }

    @Override
    public RelationshipType getType() {
        return isInterface ? RelationshipType.REALIZATION : RelationshipType.INHERITANCE;
    }

    public boolean isInterface()        { return isInterface; }
    public void    setInterface(boolean v) { this.isInterface = v; }
}
