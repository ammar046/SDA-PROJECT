package com.umlytics.domain;

import com.umlytics.enums.Visibility;
import java.util.UUID;

public class Attribute {
    private UUID attributeId;
    private String attributeName;
    private String dataType;
    private String defaultValue;
    private Visibility visibility;
    private boolean isStatic;

    public String getName() {
        return attributeName;
    }

    public String getType() {
        return dataType;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public UUID getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(UUID attributeId) {
        this.attributeId = attributeId;
    }

    public void setName(String name) {
        this.attributeName = name;
    }

    public void setType(String type) {
        this.dataType = type;
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

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String toUMLString() {
        String type = dataType == null ? "Object" : dataType;
        return (attributeName == null ? "field" : attributeName) + ": " + type;
    }
}
