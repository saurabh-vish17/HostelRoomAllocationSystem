package com.hostel.dao;

import com.hostel.config.DBConnection;
import com.hostel.model.User;

import java.sql.*;

/**
 * UserDAO — data access for admin user accounts.
 *
 * Methods:
 *   findByUsername(username)       → used by AuthService at login
 *   updatePassword(userId, hash)   → for password change after Phase 4
 */
public class UserDAO {

    // ── Map a ResultSet row to a User object ──────────────────────────────

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("id"),
            rs.getString("username"),
            rs.getString("password"),
            rs.getString("full_name"),
            rs.getString("role"),
            rs.getBoolean("is_active"),
            rs.getTimestamp("created_at"),
            rs.getTimestamp("updated_at")
        );
    }

    // ── Queries ────────────────────────────────────────────────────────────

    /**
     * Find a user by their login username.
     * Returns null if not found.
     *
     * @param username the login username
     * @return User object, or null if not found
     * @throws SQLException on database error
     */
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, password, full_name, role, " +
                     "is_active, created_at, updated_at " +
                     "FROM users WHERE username = ? AND is_active = 1";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;  // user not found
    }

    /**
     * Updates a user's password (used when migrating to BCrypt in Phase 4).
     *
     * @param userId  the user's PK
     * @param newHash BCrypt-hashed new password
     * @return true if the update affected one row
     * @throws SQLException on database error
     */
    public boolean updatePassword(int userId, String newHash) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        }
    }
}
