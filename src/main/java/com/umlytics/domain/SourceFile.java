package com.umlytics.domain;

import com.umlytics.enums.FileLanguage;
import java.util.UUID;

public class SourceFile {
    private UUID fileId;
    private UUID projectId;
    private UUID diagramId;
    private String fileName;
    private String fileContent;
    private FileLanguage language;

    public SourceFile() {
    }

    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(UUID fileId) {
        this.fileId = fileId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public UUID getDiagramId() {
        return diagramId;
    }

    public void setDiagramId(UUID diagramId) {
        this.diagramId = diagramId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileContent() {
        return fileContent;
    }

    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }

    public FileLanguage getLanguage() {
        return language;
    }

    public void setLanguage(FileLanguage language) {
        this.language = language;
    }
}
