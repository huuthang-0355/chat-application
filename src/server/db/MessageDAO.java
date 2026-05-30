package server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import common.protocol.Message;
import common.protocol.MessageType;

public class MessageDAO {

    // if receiverId null --> broadcast message
    public int saveMessage(int senderId, Integer receiverId, String content) {
        String sql = "INSERT INTO messages (sender_id, receiver_id, content) VALUES (?, ?, ?) RETURNING id";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, senderId);

            if (receiverId == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, receiverId);
            }

            ps.setString(3, content);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            return -1;
        } catch (SQLException e) {
            System.out.println("[MessageDAO] saveMessage error: " + e.getMessage());

        }

        return -1;
    }

    public List<Message> getHistory(int user1, int user2, int limit, int offset) {
        List<Message> history = new ArrayList<>();

        String sql = "SELECT m.id, u.username as sender, u.display_name as sender_display_name, m.sent_at, " +
                "CASE WHEN m.is_deleted = TRUE THEN '[This message was deleted]' ELSE m.content END as content " +
                "FROM messages m JOIN users u ON m.sender_id = u.id " +
                "WHERE ((m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?)) " +
                "AND m.sent_at > COALESCE(" +
                "    (SELECT cleared_at FROM user_conversation_clears " +
                "     WHERE user_id = ? AND conversation_type = 'PRIVATE' AND target_id = (SELECT username FROM users WHERE id = ?)), " +
                "    '1970-01-01 00:00:00'::timestamp" +
                ") " +
                "ORDER BY m.sent_at DESC LIMIT ? OFFSET ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, user1);
            ps.setInt(2, user2);
            ps.setInt(3, user2);
            ps.setInt(4, user1);
            ps.setInt(5, user1);
            ps.setInt(6, user2);
            ps.setInt(7, limit);
            ps.setInt(8, offset);
 
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Message msg = new Message(MessageType.MSG, rs.getString("sender"), String.valueOf(user2),
                            rs.getString("content"));
                    msg.setMessageId(rs.getInt("id"));
                    msg.setDisplayName(rs.getString("sender_display_name"));
                    java.sql.Timestamp sentAt = rs.getTimestamp("sent_at");
                    if (sentAt != null) {
                        msg.setTimestamp(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(sentAt));
                    }
                    history.add(msg);
                }
            }
        } catch (Exception e) {
            System.out.println("[MessageDAO] getHistory error: " + e.getMessage());
        }
        return history;
    }

    public List<Message> getPublicHistory(int userId, int limit, int offset) {
        List<Message> history = new ArrayList<>();
        String sql = "SELECT m.id, u.username as sender, u.display_name as sender_display_name, m.sent_at, " +
                "CASE WHEN m.is_deleted = TRUE THEN '[This message was deleted]' ELSE m.content END as content " +
                "FROM messages m JOIN users u ON m.sender_id = u.id " +
                "WHERE m.receiver_id IS NULL " +
                "AND m.sent_at > COALESCE(" +
                "    (SELECT cleared_at FROM user_conversation_clears " +
                "     WHERE user_id = ? AND conversation_type = 'PUBLIC' AND target_id = 'ALL'), " +
                "    '1970-01-01 00:00:00'::timestamp" +
                ") " +
                "ORDER BY m.sent_at DESC LIMIT ? OFFSET ?";

        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Message msg = new Message(MessageType.MSG, rs.getString("sender"), null,
                            rs.getString("content"));
                    msg.setMessageId(rs.getInt("id"));
                    msg.setDisplayName(rs.getString("sender_display_name"));
                    java.sql.Timestamp sentAt = rs.getTimestamp("sent_at");
                    if (sentAt != null) {
                        msg.setTimestamp(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(sentAt));
                    }
                    history.add(msg);

                }
            }
        } catch (SQLException e) {
            System.out.println("[MessageDAO] getPublicHistory error: " + e.getMessage());

        }

        return history;
    }

    public boolean deleteMessage(int messageId, int requesterId) {
        // only allow sender to delete their own message!
        String sql = "UPDATE messages SET is_deleted = TRUE WHERE id = ? AND sender_id = ?";

        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.setInt(2, requesterId);
            int rows = ps.executeUpdate();

            return rows > 0;
        } catch (SQLException e) {
            System.out.println("[MessageDAO] deleteMessage error: " + e.getMessage());
        }

        return false;
    }

    public int recordClearHistory(int userId, String conversationType, String targetId) {
        String sql = "INSERT INTO user_conversation_clears (user_id, conversation_type, target_id, cleared_at) " +
                "VALUES (?, ?, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (user_id, conversation_type, target_id) " +
                "DO UPDATE SET cleared_at = CURRENT_TIMESTAMP";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, conversationType);
            ps.setString(3, targetId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("[MessageDAO] recordClearHistory error: " + e.getMessage());
        }
        return 0;
    }

}
