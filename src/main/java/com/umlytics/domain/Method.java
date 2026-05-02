package com.umlytics.domain;

import com.umlytics.enums.Visibility;

import java.util.ArrayList;
import java.util.List;

public class Method {
    private int methodId;
    private String name;
    private String returnType;
    private List<String> parameters = new ArrayList<>();
    private Visibility visibility;
    private boolean isAbstract;

    public String getSignature() {
        return name + "(" + String.join(", ", parameters) + "): " + returnType;
    }

    public String getName() {
        return name;
    }

    public String getReturnType() {
        return returnType;
    }

    public int getMethodId() {
        return methodId;
    }

    public void setMethodId(int methodId) {
        this.methodId = methodId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public void setParameters(List<String> parameters) {
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
}
