package com.hostel.service;

import com.hostel.dao.UserDAO;
import com.hostel.model.User;
import com.hostel.util.SessionManager;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;

/**
 * AuthService — business logic for admin authentication.
 *
 * Responsibilities:
 *   1. Validate that fields are not blank before hitting the DB
 *   2. Look up the user by username
 *   3. Verify the password (supports BCrypt AND plain-text migration)
 *   4. On first plain-text login, auto-upgrade password to BCrypt in the DB
 *   5. Set / clear the login session via SessionManager
 *
 * PASSWORD MIGRATION DESIGN:
 *   Phase 2 seeded the admin with password "admin123" (plain text).
 *   On the very first login with correct plain-text credentials, this
 *   service silently re-hashes and saves the BCrypt version.
 *   After that, every login uses BCrypt — the plain-text version is gone.
 *   This is transparent to the user.
 */
public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    // ── Login ──────────────────────────────────────────────────────────────

    /**
     * Attempts to authenticate the admin.
     *
     * @param username entered username (trimmed internally)
     * @param password entered password (not trimmed — spaces in passwords are valid)
     * @return the authenticated User object (also stored in SessionManager)
     * @throws AuthException with a user-friendly message on any failure
     */
    public User login(String username, String password) throws AuthException {

        // ── 1. Field validation ────────────────────────────────────────────
        if (username == null || username.trim().isEmpty()) {
            throw new AuthException("Please enter your username.");
        }
        if (password == null || password.isEmpty()) {
            throw new AuthException("Please enter your password.");
        }

        String trimmedUsername = username.trim();

        // ── 2. Look up user ────────────────────────────────────────────────
        User user;
        try {
            user = userDAO.findByUsername(trimmedUsername);
        } catch (SQLException e) {
            throw new AuthException(
                "Database error during login. Please try again.\n" +
                "Details: " + e.getMessage(), e);
        }

        // Generic error — do NOT tell the attacker whether the username exists
        if (user == null) {
            throw new AuthException("Invalid username or password.");
        }

        // ── 3. Account active check ────────────────────────────────────────
        if (!user.isActive()) {
            throw new AuthException("This account has been deactivated.\nPlease contact the system administrator.");
        }

        // ── 4. Password verification ───────────────────────────────────────
        boolean matched = verifyPassword(password, user.getPassword());
        if (!matched) {
            throw new AuthException("Invalid username or password.");
        }

        // ── 5. Auto-upgrade plain-text → BCrypt ────────────────────────────
        String stored = user.getPassword();
        if (!isBcryptHash(stored)) {
            try {
                String newHash = BCrypt.hashpw(password, BCrypt.gensalt(12));
                boolean updated = userDAO.updatePassword(user.getId(), newHash);
                if (updated) {
                    user.setPassword(newHash);
                    System.out.println("[AuthService] Password upgraded to BCrypt for user: "
                        + trimmedUsername);
                }
            } catch (SQLException e) {
                // Non-fatal — user can still log in even if upgrade fails
                System.err.println("[AuthService] BCrypt upgrade failed: " + e.getMessage());
            }
        }

        // ── 6. Create session ──────────────────────────────────────────────
        SessionManager.setCurrentUser(user);

        System.out.println("[AuthService] Login successful: " + trimmedUsername);
        return user;
    }

    // ── Logout ─────────────────────────────────────────────────────────────

    /**
     * Ends the current session.
     * Call this from the Dashboard's logout action.
     */
    public void logout() {
        String username = SessionManager.isLoggedIn()
            ? SessionManager.getCurrentUser().getUsername()
            : "unknown";
        SessionManager.clearSession();
        System.out.println("[AuthService] Logged out: " + username);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Verifies a plain-text password against the stored value.
     * Handles both BCrypt-hashed and plain-text stored passwords.
     *
     * @param plainText the password the user typed
     * @param stored    the value from the database
     * @return true if they match
     */
    private boolean verifyPassword(String plainText, String stored) {
        if (isBcryptHash(stored)) {
            try {
                return BCrypt.checkpw(plainText, stored);
            } catch (Exception e) {
                // Malformed hash in DB — treat as no match
                System.err.println("[AuthService] BCrypt check error: " + e.getMessage());
                return false;
            }
        }
        // Plain-text fallback (Phase 2 seed data)
        return plainText.equals(stored);
    }

    /**
     * Returns true if the string looks like a BCrypt hash.
     * BCrypt hashes always start with "$2a$" or "$2b$".
     */
    private boolean isBcryptHash(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$"));
    }
}
