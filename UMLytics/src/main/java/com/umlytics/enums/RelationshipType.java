package com.umlytics.enums;

public enum RelationshipType {
    INHERITANCE("Inheritance (extends)"),
    COMPOSITION("Composition"),
    AGGREGATION("Aggregation"),
    DEPENDENCY("Dependency"),
    REALIZATION("Realization (implements)"),
    ASSOCIATION("Association");

    private final String displayName;

    RelationshipType(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }

    @Override
    public String toString() { return displayName; }
}
