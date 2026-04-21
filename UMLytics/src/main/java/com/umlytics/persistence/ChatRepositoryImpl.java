package com.umlytics.persistence;

import com.umlytics.domain.ChatMessage;
import com.umlytics.enums.SenderType;
import com.umlytics.interfaces.IChatRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * SQLite implementation of IChatRepository.
 * GRASP: Pure Fabrication
 */
public class ChatRepositoryImpl implements IChatRepository {

    private final DatabaseManager db = DatabaseManager.getInstance();
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void save(ChatMessage m) {
        if (m == null) throw new IllegalArgumentException("ChatMessage cannot be null");
        String sql = "INSERT INTO chat_messages (project_id, content, sender, timestamp) VALUES (?,?,?,?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, m.getProjectId());
            ps.setString(2, m.getContent());
            ps.setString(3, m.getSender().name());
            ps.setString(4, SDF.format(m.getTimestamp() != null ? m.getTimestamp() : new Date()));
            ps.executeUpdate();
            try (Statement stmt = conn.createStatement();
                 ResultSet keys = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (keys.next()) m.setMessageId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save chat message: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ChatMessage> findByProject(int projectId) {
        List<ChatMessage> list = new ArrayList<>();
        String sql = "SELECT * FROM chat_messages WHERE project_id=? ORDER BY timestamp ASC";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load chat messages: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM chat_messages WHERE message_id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete message: " + e.getMessage(), e);
        }
    }

    private ChatMessage mapRow(ResultSet rs) throws SQLException {
        ChatMessage m = new ChatMessage();
        m.setMessageId(rs.getInt("message_id"));
        m.setProjectId(rs.getInt("project_id"));
        m.setContent(rs.getString("content"));
        m.setSender(SenderType.valueOf(rs.getString("sender")));
        try {
            m.setTimestamp(SDF.parse(rs.getString("timestamp")));
        } catch (Exception ignored) {}
        return m;
    }
}
