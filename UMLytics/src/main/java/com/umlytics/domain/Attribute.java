package com.umlytics.domain;

import com.umlytics.enums.Visibility;

/**
 * Represents an attribute (field) of a UML class.
 * GRASP: Information Expert
 */
public class Attribute {
    private int        attributeId;
    private String     name;
    private String     type;
    private Visibility visibility;
    private boolean    isStatic;

    public Attribute() {}

    public Attribute(int id, String name, String type, Visibility visibility, boolean isStatic) {
        this.attributeId = id;
        this.name        = name;
        this.type        = type;
        this.visibility  = visibility;
        this.isStatic    = isStatic;
    }

    public int        getAttributeId()     { return attributeId; }
    public void       setAttributeId(int id) { this.attributeId = id; }
    public String     getName()            { return name; }
    public void       setName(String n)    { this.name = n; }
    public String     getType()            { return type; }
    public void       setType(String t)    { this.type = t; }
    public Visibility getVisibility()      { return visibility; }
    public void       setVisibility(Visibility v) { this.visibility = v; }
    public boolean    isStatic()           { return isStatic; }
    public void       setStatic(boolean s) { this.isStatic = s; }

    /** e.g. "- name: String" */
    public String toUMLString() {
        String prefix = (visibility != null ? visibility.getSymbol() : "+");
        String staticMark = isStatic ? "{static} " : "";
        return prefix + " " + staticMark + name + " : " + type;
    }

    @Override
    public String toString() { return toUMLString(); }
}
