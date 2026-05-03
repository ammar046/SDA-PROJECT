package com.umlytics.domain;

import com.umlytics.enums.SenderType;

import java.time.LocalDateTime;
import java.util.UUID;

public class ChatMessage {
    private UUID messageId;
    private String content;
    private SenderType sender;
    private LocalDateTime timestamp;
    private UUID projectId;
    private UUID classId;

    public String getContent() {
        return content;
    }

    public SenderType getSender() {
        return sender;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setSender(SenderType sender) {
        this.sender = sender;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public UUID getClassId() {
        return classId;
    }

    public void setClassId(UUID classId) {
        this.classId = classId;
    }
}
