package com.umlytics.domain;

import com.umlytics.enums.ClassType;
import com.umlytics.enums.Visibility;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConceptualClass {
    private UUID classId;
    private UUID diagramId;
    private String name;
    private ClassType classType = ClassType.ENTITY;
    private Visibility visibility = Visibility.PUBLIC;
    private double positionX;
    private double positionY;
    private String headerColor = "Blue";
    private String borderColor = "Blue";
    private double memberFontSize = 12.0;
    private double classWidth = 200.0;
    private double classHeight = 140.0;
    private final List<Attribute> attributes = new ArrayList<>();
    private final List<Method> methods = new ArrayList<>();

    public void addAttribute(Attribute a) {
        attributes.add(a);
    }

    public void removeAttribute(UUID id) {
        attributes.removeIf(a -> id.equals(a.getAttributeId()));
    }

    public void addMethod(Method m) {
        methods.add(m);
    }

    public void removeMethod(UUID id) {
        methods.removeIf(m -> id.equals(m.getMethodId()));
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public List<Method> getMethods() {
        return methods;
    }

    public UUID getClassId() {
        return classId;
    }

    public void setClassId(UUID classId) {
        this.classId = classId;
    }

    public void setClassId(int classId) {
        this.classId = UUID.nameUUIDFromBytes(("legacy-class-" + classId).getBytes());
    }

    public UUID getDiagramId() {
        return diagramId;
    }

    public void setDiagramId(UUID diagramId) {
        this.diagramId = diagramId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ClassType getClassType() {
        return classType;
    }

    public void setClassType(ClassType classType) {
        this.classType = classType == null ? ClassType.ENTITY : classType;
    }

    public boolean isAbstract() {
        return classType == ClassType.ABSTRACT;
    }

    public boolean isInterface() {
        return classType == ClassType.INTERFACE;
    }

    public boolean isEnum() {
        return classType == ClassType.ENUM;
    }

    public void setAbstract(boolean value) {
        if (value) {
            this.classType = ClassType.ABSTRACT;
        } else if (this.classType == ClassType.ABSTRACT) {
            this.classType = ClassType.ENTITY;
        }
    }

    public void setInterface(boolean value) {
        if (value) {
            this.classType = ClassType.INTERFACE;
        } else if (this.classType == ClassType.INTERFACE) {
            this.classType = ClassType.ENTITY;
        }
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility == null ? Visibility.PUBLIC : visibility;
    }

    public double getPositionX() {
        return positionX;
    }

    public void setPositionX(double positionX) {
        this.positionX = positionX;
    }

    public double getPositionY() {
        return positionY;
    }

    public void setPositionY(double positionY) {
        this.positionY = positionY;
    }

    public String getHeaderColor() {
        return headerColor;
    }

    public void setHeaderColor(String headerColor) {
        this.headerColor = headerColor;
    }

    public String getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(String borderColor) {
        this.borderColor = borderColor;
    }

    public double getMemberFontSize() {
        return memberFontSize;
    }

    public void setMemberFontSize(double memberFontSize) {
        this.memberFontSize = memberFontSize;
    }

    public double getClassWidth() {
        return classWidth;
    }

    public void setClassWidth(double classWidth) {
        this.classWidth = classWidth;
    }

    public double getClassHeight() {
        return classHeight;
    }

    public void setClassHeight(double classHeight) {
        this.classHeight = classHeight;
    }
}
