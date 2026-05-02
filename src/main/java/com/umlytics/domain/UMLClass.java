package com.umlytics.domain;

import java.util.ArrayList;
import java.util.List;

public class UMLClass {
    private int classId;
    private String name;
    private boolean isAbstract;
    private boolean isInterface;
    private double positionX;
    private double positionY;
    private String headerColor = "Blue";
    private String borderColor = "Blue";
    private double memberFontSize = 12.0;
    private final List<Attribute> attributes = new ArrayList<>();
    private final List<Method> methods = new ArrayList<>();

    public void addAttribute(Attribute a) {
        attributes.add(a);
    }

    public void removeAttribute(int id) {
        attributes.removeIf(a -> a.getAttributeId() == id);
    }

    public void addMethod(Method m) {
        methods.add(m);
    }

    public void removeMethod(int id) {
        methods.removeIf(m -> m.getMethodId() == id);
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public List<Method> getMethods() {
        return methods;
    }

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract(boolean anAbstract) {
        isAbstract = anAbstract;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public void setInterface(boolean anInterface) {
        isInterface = anInterface;
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
}
