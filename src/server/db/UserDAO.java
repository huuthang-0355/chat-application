package server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    public boolean createUser(String username, String passwordHash) {
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            // PostgreSQL unique violation SQLState = "23505"
            if ("23505".equals(e.getSQLState())) {
                System.out.println("[UserDAO]: User '" + username + "' already exists.");
            } else {
                System.out.println("[UserDAO] createUser error: " + e.getMessage());
            }
        }

        return false;
    }

    public int findUserByUsername(String username) {
        String sql = "SELECT id FROM users WHERE username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("id");
            }

        } catch (SQLException e) {
            System.out.println("[UserDAO] findUserByUsername error: " + e.getMessage());
        }

        return -1;
    }
}
