package server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    public boolean createUser(String username, String passwordHash, String displayName) {
        String sql = "INSERT INTO users (username, password_hash, display_name) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, displayName);

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

    public String getDisplayNameByUsername(String username) {
        String sql = "SELECT display_name FROM users WHERE username = ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getString("display_name");
            }
        } catch (SQLException e) {
            System.out.println("[UserDAO] getDisplayNameByUsername error: " + e.getMessage());
        }
        return null;
    }

    public String getDisplayNameById(int id) {
        String sql = "SELECT display_name FROM users WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getString("display_name");
            }
        } catch (SQLException e) {
            System.out.println("[UserDAO] getDisplayNameById error: " + e.getMessage());
        }
        return null;
    }

    public String getPasswordHash(String username) {
        String sql = "SELECT password_hash FROM users WHERE username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery();) {
                if (rs.next())
                    return rs.getString("password_hash");
            }

            return null;
        } catch (SQLException e) {
            System.out.println("[UserDAO] getPasswordHash error: " + e.getMessage());
            return null;
        }
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

    public String getUsernameById(int id) {
        String sql = "SELECT username FROM users WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getString(1);
            }

            return null;
        } catch (SQLException e) {
            System.out.println("[UserDAO] getUsernameById error: " + e.getMessage());
        }

        return null;
    }
}
