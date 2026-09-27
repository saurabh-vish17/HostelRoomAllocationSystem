package com.hostel.service;

/**
 * AuthException — thrown by AuthService when login fails.
 *
 * Using a custom exception (rather than raw SQLException or RuntimeException)
 * lets the LoginFrame catch only auth-specific failures and display
 * a user-friendly message — while still allowing unexpected errors to
 * propagate separately.
 *
 * Examples of messages:
 *   "Username cannot be empty."
 *   "Invalid username or password."
 *   "Account is deactivated. Contact administrator."
 *   "Database error during login. Please try again."
 */
public class AuthException extends Exception {

    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
