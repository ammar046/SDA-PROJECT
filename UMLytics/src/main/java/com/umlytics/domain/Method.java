package com.umlytics.domain;

import com.umlytics.enums.Visibility;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a method of a UML class.
 * GRASP: Information Expert
 */
public class Method {
    private int          methodId;
    private String       name;
    private String       returnType;
    private List<String> parameters = new ArrayList<>();
    private Visibility   visibility;
    private boolean      isAbstract;

    public Method() {}

    public Method(int id, String name, String returnType, Visibility visibility, boolean isAbstract) {
        this.methodId   = id;
        this.name       = name;
        this.returnType = returnType;
        this.visibility = visibility;
        this.isAbstract = isAbstract;
    }

    /** e.g. "+ getName() : String" */
    public String getSignature() {
        String prefix = (visibility != null ? visibility.getSymbol() : "+");
        String params = parameters.stream().collect(Collectors.joining(", "));
        String abstractMark = isAbstract ? "{abstract} " : "";
        return prefix + " " + abstractMark + name + "(" + params + ") : " + returnType;
    }

    public int        getMethodId()       { return methodId; }
    public void       setMethodId(int id) { this.methodId = id; }
    public String     getName()           { return name; }
    public void       setName(String n)   { this.name = n; }
    public String     getReturnType()     { return returnType; }
    public void       setReturnType(String r) { this.returnType = r; }
    public List<String> getParameters()   { return new ArrayList<>(parameters); }
    public void       setParameters(List<String> p) { this.parameters = p != null ? p : new ArrayList<>(); }
    public Visibility getVisibility()     { return visibility; }
    public void       setVisibility(Visibility v) { this.visibility = v; }
    public boolean    isAbstract()        { return isAbstract; }
    public void       setAbstract(boolean a) { this.isAbstract = a; }

    @Override
    public String toString() { return getSignature(); }
}
