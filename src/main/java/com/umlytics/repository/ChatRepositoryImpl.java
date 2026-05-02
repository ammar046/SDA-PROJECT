package com.umlytics.repository;

import com.umlytics.db.DatabaseManager;
import com.umlytics.domain.ChatMessage;
import com.umlytics.enums.SenderType;
import com.umlytics.exceptions.DatabaseException;
import com.umlytics.interfaces.IChatRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// GRASP: Pure Fabrication
public class ChatRepositoryImpl implements IChatRepository {
    public ChatRepositoryImpl() {
    }

    @Override
    public void save(ChatMessage m) {
        String sql = "INSERT INTO chat_message(message_id, project_id, class_id, sender_role, content, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
        if (m.getTimestamp() == null) {
            m.setTimestamp(LocalDateTime.now());
        }
        if (m.getSender() == null) {
            m.setSender(SenderType.USER);
        }
        if (m.getMessageId() == null) {
            m.setMessageId(UUID.randomUUID());
        }
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            ps.setString(1, m.getMessageId().toString());
            ps.setString(2, m.getProjectId() == null ? null : m.getProjectId().toString());
            ps.setString(3, m.getClassId() == null ? null : m.getClassId().toString());
            ps.setString(4, m.getSender().name());
            ps.setString(5, m.getContent());
            ps.setString(6, m.getTimestamp().toString());
            ps.executeUpdate();
            conn.commit();
        } catch (Exception e) {
            throw new DatabaseException("Failed to save chat message.", e);
        }
    }

    @Override
    public List<ChatMessage> findByProject(UUID projectId) {
        String sql = "SELECT * FROM chat_message WHERE project_id = ? ORDER BY timestamp ASC";
        List<ChatMessage> messages = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projectId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ChatMessage message = new ChatMessage();
                    message.setMessageId(UUID.fromString(rs.getString("message_id")));
                    message.setProjectId(UUID.fromString(rs.getString("project_id")));
                    String classId = rs.getString("class_id");
                    if (classId != null && !classId.isBlank()) {
                        message.setClassId(UUID.fromString(classId));
                    }
                    message.setContent(rs.getString("content"));
                    message.setSender(SenderType.valueOf(rs.getString("sender_role")));
                    message.setTimestamp(LocalDateTime.parse(rs.getString("timestamp")));
                    messages.add(message);
                }
            }
            return messages;
        } catch (Exception e) {
            throw new DatabaseException("Failed to fetch chat history.", e);
        }
    }

    @Override
    public void delete(UUID id) {
        String sql = "DELETE FROM chat_message WHERE message_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            ps.setString(1, id.toString());
            ps.executeUpdate();
            conn.commit();
        } catch (Exception e) {
            throw new DatabaseException("Failed to delete chat message.", e);
        }
    }
}
