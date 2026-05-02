package com.umlytics.domain;

import java.util.UUID;

public class SoftwareDesigner {
    private UUID designerId;
    private String name;
    private String email;
    private String preferences;

    public SoftwareDesigner() {
    }

    public UUID getDesignerId() {
        return designerId;
    }

    public void setDesignerId(UUID designerId) {
        this.designerId = designerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPreferences() {
        return preferences;
    }

    public void setPreferences(String preferences) {
        this.preferences = preferences;
    }
}
