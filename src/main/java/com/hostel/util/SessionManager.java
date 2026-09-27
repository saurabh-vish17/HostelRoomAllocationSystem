package com.hostel.util;

import com.hostel.model.User;

/**
 * SessionManager — static store for the currently logged-in admin user.
 *
 * This is a simple utility class (no instantiation needed).
 * It acts as an in-memory session — once an admin logs in, any UI panel
 * can call SessionManager.getCurrentUser() to get their details without
 * having to pass the User object through every constructor.
 *
 * Flow:
 *   Login:  AuthService calls setCurrentUser(user) after successful auth
 *   Access: Any panel calls getCurrentUser() or getDisplayName()
 *   Logout: AuthService calls clearSession()
 *   Guard:  DashboardFrame checks isLoggedIn() on creation
 */
public class SessionManager {

    /** The currently signed-in admin. null = nobody is logged in. */
    private static User currentUser = null;

    // Private constructor — this is a static utility class
    private SessionManager() {}

    // ── Write ──────────────────────────────────────────────────────────────

    /**
     * Store the authenticated user after a successful login.
     * Called by AuthService.login().
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    /**
     * Removes the session.
     * Called by AuthService.logout().
     */
    public static void clearSession() {
        currentUser = null;
    }

    // ── Read ───────────────────────────────────────────────────────────────

    /**
     * Returns the currently logged-in User, or null if nobody is logged in.
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * Returns true if a user is currently logged in.
     */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Convenience: returns the username of the logged-in admin.
     * Returns "Unknown" if no session is active.
     */
    public static String getUsername() {
        return currentUser != null ? currentUser.getUsername() : "Unknown";
    }

    /**
     * Convenience: returns the full name of the logged-in admin.
     * Returns "Unknown" if no session is active.
     */
    public static String getFullName() {
        return currentUser != null ? currentUser.getFullName() : "Unknown";
    }

    /**
     * Convenience: returns "Full Name (username)" for display in the header.
     */
    public static String getDisplayName() {
        if (currentUser == null) return "Not logged in";
        return currentUser.getFullName() + "  (" + currentUser.getUsername() + ")";
    }
}
