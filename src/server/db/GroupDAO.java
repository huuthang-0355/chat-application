package server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

    public boolean saveGroupMessage(int groupId, int senderId, String content) {
        String sql = "INSERT INTO group_messages (group_id, sender_id, content) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, senderId);
            ps.setString(3, content);

            int rows = ps.executeUpdate();

            return rows > 0;
        } catch (SQLException e) {
            System.out.println("[GroupDAO] saveGroupMessage error: " + e.getMessage());
        }

        return false;
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

}
