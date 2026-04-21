package com.umlytics.domain;

import com.umlytics.enums.Visibility;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single UML class node.
 * GRASP: Information Expert
 */
public class UMLClass {
    private int classId;
    private String name;
    private boolean isAbstract;
    private boolean isInterface;
    private double positionX;
    private double positionY;
    /** e.g. "Note", "Enumeration", or "" for plain class */
    private String stereotype = "";
    /** Transient UI state — not persisted. Used for rubber-band selection. */
    private transient boolean selected = false;

    private final List<Attribute> attributes = new ArrayList<>();
    private final List<Method>    methods    = new ArrayList<>();

    public UMLClass() {}

    public UMLClass(int classId, String name) {
        this.classId = classId;
        this.name    = name;
    }

    // ---- Attribute management ----
    public void addAttribute(Attribute a) {
        if (a != null && attributes.stream().noneMatch(x -> x.getAttributeId() == a.getAttributeId()))
            attributes.add(a);
    }

    public void removeAttribute(int id) {
        attributes.removeIf(a -> a.getAttributeId() == id);
    }

    public List<Attribute> getAttributes() { return new ArrayList<>(attributes); }

    // ---- Method management ----
    public void addMethod(Method m) {
        if (m != null && methods.stream().noneMatch(x -> x.getMethodId() == m.getMethodId()))
            methods.add(m);
    }

    public void removeMethod(int id) {
        methods.removeIf(m -> m.getMethodId() == id);
    }

    public List<Method> getMethods() { return new ArrayList<>(methods); }

    // ---- Getters / Setters ----
    public int    getClassId()       { return classId; }
    public void   setClassId(int id) { this.classId = id; }
    public String getName()          { return name; }
    public void   setName(String n)  { this.name = n; }
    public boolean isAbstract()      { return isAbstract; }
    public void    setAbstract(boolean v) { this.isAbstract = v; }
    public boolean isInterface()     { return isInterface; }
    public void    setInterface(boolean v) { this.isInterface = v; }
    public double  getPositionX()    { return positionX; }
    public void    setPositionX(double x) { this.positionX = x; }
    public double  getPositionY()    { return positionY; }
    public void    setPositionY(double y) { this.positionY = y; }
    public String  getStereotype()   { return stereotype != null ? stereotype : ""; }
    public void    setStereotype(String s) { this.stereotype = s != null ? s : ""; }
    public boolean isSelected()      { return selected; }
    public void    setSelected(boolean s) { this.selected = s; }

    @Override
    public String toString() { return name; }
}
