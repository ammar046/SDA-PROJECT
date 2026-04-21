package com.umlytics.enums;

public enum ExportFormat {
    PNG("PNG Image", "*.png"),
    PDF("PDF Document", "*.pdf"),
    SVG("SVG Vector", "*.svg");

    private final String displayName;
    private final String extension;

    ExportFormat(String displayName, String extension) {
        this.displayName = displayName;
        this.extension = extension;
    }

    public String getDisplayName() { return displayName; }
    public String getExtension()   { return extension; }

    @Override
    public String toString() { return displayName; }
}
