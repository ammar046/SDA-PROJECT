package com.umlytics.domain;

import com.umlytics.enums.Visibility;

public class Attribute {
    private int attributeId;
    private String name;
    private String type;
    private Visibility visibility;
    private boolean isStatic;

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public int getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(int attributeId) {
        this.attributeId = attributeId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }
}
