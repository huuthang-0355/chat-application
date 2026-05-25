package server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import common.protocol.Message;
import common.protocol.MessageType;

public class GroupDAO {

    // return new group's id, or -1 on failure
    public int createGroup(String name, int creatorId) {
        String sql = "INSERT INTO groups (name, created_by) VALUES (?, ?) RETURNING id";

        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, creatorId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.out.println("[GroupDAO] createGroup error: " + e.getMessage());
        }

        return -1;
    }

    public boolean addMember(int groupId, int userId) {
        String sql = "INSERT INTO group_members (group_id, user_id) VALUES (?, ?)";

        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);

            int rows = ps.executeUpdate();

            return rows > 0;
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                System.out.println("[GroupDAO] User already exists in group.");
            } else {
                System.out.println("[GroupDAO] addmember error: " + e.getMessage());
            }
        }

        return false;
    }

    public boolean removeMember(int groupId, int userId) {
        String sql = "DELETE FROM group_members WHERE group_id = ? AND user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);

            int rows = ps.executeUpdate();

            return rows > 0;
        } catch (SQLException e) {
            System.out.println("[GroupDAO] remomveMemeber error: " + e.getMessage());
        }

        return false;
    }

    public List<Integer> getMemberIds(int groupId) {
        String sql = "SELECT user_id FROM group_members WHERE group_id = ?";

        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            List<Integer> idList = new ArrayList<>();
            ps.setInt(1, groupId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    idList.add(rs.getInt(1));
                }
            }

            return idList;
        } catch (SQLException e) {
            System.out.println("[GroupDAO] getMemberIds error: " + e.getMessage());
        }

        return new ArrayList<>();
    }

    public int getGroupIdByName(String name) {
        String sql = "SELECT id FROM groups WHERE name = ?";

        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1);
            }

            return -1;
        } catch (SQLException e) {
            System.out.println("[GroupDAO] getGroupIdByName error: " + e.getMessage());
        }

        return -1;
    }

    // Returns a list of "name:id" for all groups the user belongs to
    public List<String> getUserGroups(int userId) {
        String sql = "SELECT g.id, g.name FROM groups g "
                + "JOIN group_members gm ON g.id = gm.group_id "
                + "WHERE gm.user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);

            List<String> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // build str "name:id"
                    String val = rs.getString(2) + ":" + String.valueOf(rs.getInt(1));

                    result.add(val);
                }
            }

            return result;
        } catch (SQLException e) {
            System.out.println("[GroupDAO] getUserGroups error: " + e.getMessage());
        }

        return new ArrayList<>();
    }

    public int saveGroupMessage(int groupId, int senderId, String content) {
        String sql = "INSERT INTO group_messages (group_id, sender_id, content) VALUES (?, ?, ?) RETURNING id";

        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, senderId);
            ps.setString(3, content);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.out.println("[GroupDAO] saveGroupMessage error: " + e.getMessage());
        }

        return -1;
    }

    public boolean isMember(int groupId, int userId) {
        String sql = "SELECT 1 FROM group_members WHERE group_id = ? AND user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return true;
            }

            return false;
        } catch (SQLException e) {
            System.out.println("[GroupDAO] isMember error: " + e.getMessage());
        }

        return false;
    }

    public List<Message> getGroupHistory(int groupId, int userId, int limit, int offset) {
        List<Message> history = new ArrayList<>();

        String sql = "SELECT gm.id, u.username as sender, " +
                "CASE WHEN gm.is_deleted = TRUE THEN '[This message was deleted]' ELSE gm.content END as content " +
                "FROM group_messages gm JOIN users u ON gm.sender_id = u.id " +
                "WHERE gm.group_id = ? " +
                "AND gm.sent_at > COALESCE(" +
                "    (SELECT cleared_at FROM user_conversation_clears " +
                "     WHERE user_id = ? AND conversation_type = 'GROUP' AND target_id = ?), " +
                "    '1970-01-01 00:00:00'::timestamp" +
                ") " +
                "ORDER BY gm.sent_at DESC LIMIT ? OFFSET ?";

        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            ps.setString(3, String.valueOf(groupId));
            ps.setInt(4, limit);
            ps.setInt(5, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Message msg = new Message(MessageType.GROUP_MSG, rs.getString("sender"), String.valueOf(groupId),
                            rs.getString("content"));
                    msg.setMessageId(rs.getInt("id"));
                    history.add(msg);
                }
            }

        } catch (SQLException e) {
            System.out.println("[GroupDAO] getGroupHistory error: " + e.getMessage());
        }

        return history;
    }

    public boolean deleteGroupMessage(int messageId, int requesterId, int groupId) {
        // only allow sender to delete their own message!
        String sql = "UPDATE group_messages SET is_deleted = TRUE WHERE id = ? AND sender_id = ? AND group_id = ?";

        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.setInt(2, requesterId);
            ps.setInt(3, groupId);
            int rows = ps.executeUpdate();

            return rows > 0;
        } catch (SQLException e) {
            System.out.println("[GroupDAO] deleteGroupMessage error: " + e.getMessage());
        }

        return false;
    }
    public List<String> getGroupMembers(int groupId) {
        String sql = "SELECT u.username FROM users u "
                   + "JOIN group_members gm ON u.id = gm.user_id "
                   + "WHERE gm.group_id = ? "
                   + "ORDER BY u.username ASC";
        List<String> members = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(rs.getString("username"));
                }
            }
        } catch (SQLException e) {
            System.out.println("[GroupDAO] getGroupMembers error: " + e.getMessage());
        }
        return members;
    }

}
