package com.umlytics.domain;

import com.umlytics.enums.SenderType;

import java.util.Date;

public class ChatMessage {
    private int messageId;
    private String content;
    private SenderType sender;
    private Date timestamp;
    private int projectId;

    public String getContent() {
        return content;
    }

    public SenderType getSender() {
        return sender;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setSender(SenderType sender) {
        this.sender = sender;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }
}
