package com.umlytics.domain;

import com.umlytics.enums.Visibility;

import java.util.UUID;

public class Method {
    private UUID methodId;
    private String methodName;
    private String returnType;
    private String parameters;
    private Visibility visibility;
    private boolean isAbstract;

    public String getSignature() {
        return methodName + "(" + (parameters != null ? parameters : "") + "): " + returnType;
    }

    public String getName() {
        return methodName;
    }

    public String getReturnType() {
        return returnType;
    }

    public UUID getMethodId() {
        return methodId;
    }

    public void setMethodId(UUID methodId) {
        this.methodId = methodId;
    }

    public void setName(String name) {
        this.methodName = name;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract(boolean anAbstract) {
        isAbstract = anAbstract;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
}
