package com.umlytics.enums;

public enum SourceType {
    NL("Natural Language"),
    CODE("Source Code"),
    UPLOAD("Uploaded Image"),
    MANUAL("Manual Edit");

    private final String displayName;

    SourceType(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }

    @Override
    public String toString() { return displayName; }
}
