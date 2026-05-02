package com.umlytics.repository;

import com.umlytics.db.DatabaseManager;
import com.umlytics.domain.ChatMessage;
import com.umlytics.enums.SenderType;
import com.umlytics.exceptions.DatabaseException;
import com.umlytics.interfaces.IChatRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// GRASP: Pure Fabrication
public class ChatRepositoryImpl implements IChatRepository {
    private final Connection connection;

    public ChatRepositoryImpl() {
        this.connection = null;
    }

    @Override
    public void save(ChatMessage m) {
        String sql = "INSERT INTO chat_messages(project_id, content, sender, timestamp) VALUES (?, ?, ?, ?)";
        if (m.getTimestamp() == null) {
            m.setTimestamp(new Date());
        }
        if (m.getSender() == null) {
            m.setSender(SenderType.USER);
        }
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            conn.setAutoCommit(false);
            ps.setInt(1, m.getProjectId());
            ps.setString(2, m.getContent());
            ps.setString(3, m.getSender().name());
            ps.setString(4, m.getTimestamp().toInstant().toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    m.setMessageId(rs.getInt(1));
                }
            }
            conn.commit();
        } catch (Exception e) {
            throw new DatabaseException("Failed to save chat message.", e);
        }
    }

    @Override
    public List<ChatMessage> findByProject(int pid) {
        String sql = "SELECT * FROM chat_messages WHERE project_id = ? ORDER BY timestamp ASC";
        List<ChatMessage> messages = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ChatMessage message = new ChatMessage();
                    message.setMessageId(rs.getInt("message_id"));
                    message.setProjectId(rs.getInt("project_id"));
                    message.setContent(rs.getString("content"));
                    message.setSender(SenderType.valueOf(rs.getString("sender")));
                    message.setTimestamp(Date.from(Instant.parse(rs.getString("timestamp"))));
                    messages.add(message);
                }
            }
            return messages;
        } catch (Exception e) {
            throw new DatabaseException("Failed to fetch chat history.", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM chat_messages WHERE message_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            ps.setInt(1, id);
            ps.executeUpdate();
            conn.commit();
        } catch (Exception e) {
            throw new DatabaseException("Failed to delete chat message.", e);
        }
    }
}
