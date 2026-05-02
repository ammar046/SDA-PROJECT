package com.umlytics.domain;

import com.umlytics.enums.ImageFormat;
import java.time.LocalDateTime;
import java.util.UUID;

public class DiagramImage {
    private UUID imageId;
    private UUID diagramId;
    private String fileName;
    private ImageFormat imageFormat;
    private byte[] imageData;
    private LocalDateTime uploadDate;

    public DiagramImage() {
    }

    public UUID getImageId() {
        return imageId;
    }

    public void setImageId(UUID imageId) {
        this.imageId = imageId;
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

    public ImageFormat getImageFormat() {
        return imageFormat;
    }

    public void setImageFormat(ImageFormat imageFormat) {
        this.imageFormat = imageFormat;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }
}
