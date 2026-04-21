package com.umlytics.domain;

import com.umlytics.enums.SenderType;
import java.util.Date;

/**
 * Represents a single message in the AI chat history.
 * GRASP: Information Expert
 */
public class ChatMessage {
    private int        messageId;
    private String     content;
    private SenderType sender;
    private Date       timestamp;
    private int        projectId;

    public ChatMessage() {}

    public ChatMessage(int messageId, String content, SenderType sender, Date timestamp, int projectId) {
        this.messageId = messageId;
        this.content   = content;
        this.sender    = sender;
        this.timestamp = timestamp;
        this.projectId = projectId;
    }

    public int        getMessageId()        { return messageId; }
    public void       setMessageId(int id)  { this.messageId = id; }
    public String     getContent()          { return content; }
    public void       setContent(String c)  { this.content = c; }
    public SenderType getSender()           { return sender; }
    public void       setSender(SenderType s) { this.sender = s; }
    public Date       getTimestamp()        { return timestamp; }
    public void       setTimestamp(Date t)  { this.timestamp = t; }
    public int        getProjectId()        { return projectId; }
    public void       setProjectId(int pid) { this.projectId = pid; }

    public boolean isFromAI() { return sender == SenderType.AI; }

    @Override
    public String toString() {
        return "[" + sender + "] " + content;
    }
}
