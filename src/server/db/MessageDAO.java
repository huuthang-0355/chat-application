package server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class MessageDAO {

    // if receiverId null --> broadcast message
    public boolean saveMessage(int senderId, Integer receiverId, String content) {
        String sql = "INSERT INTO messages (sender_id, receiver_id, content) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, senderId);

            if (receiverId == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, receiverId);
            }

            ps.setString(3, content);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();

        }

        return false;
    }
}
