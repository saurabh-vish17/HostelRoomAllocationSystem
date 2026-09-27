package com.hostel;

import com.hostel.config.DBConnection;
import com.hostel.ui.LoginFrame;

import javax.swing.*;

/**
 * Main — application entry point.
 *
 * Phase 4 onwards: launches the Login screen.
 * (The Phase 3 DB test runner has been replaced now that the UI is live.)
 *
 * Startup sequence:
 *   1. Set system look-and-feel
 *   2. Verify DB connection (show error dialog if unreachable)
 *   3. Launch LoginFrame on the Event Dispatch Thread
 */
public class Main {

    public static void main(String[] args) {

        // ── 1. Modern Swing Look & Feel (FlatLaf) ───────────────────────
        com.hostel.ui.UIUtil.setupTheme();

        // ── 2. Pre-flight: verify DB connection ───────────────────────────
        if (!DBConnection.testConnection()) {
            SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(
                    null,
                    "<html><b>Cannot connect to the database.</b><br><br>" +
                    "Please check:<br>" +
                    "  • MySQL is running on localhost:3306<br>" +
                    "  • Database <b>hostel_management</b> exists<br>" +
                    "  • Credentials in <b>src/main/resources/db.properties</b> are correct<br><br>" +
                    "The application will now exit.</html>",
                    "Database Connection Error",
                    JOptionPane.ERROR_MESSAGE
                )
            );
            return;
        }

        // ── 3. Launch Login UI on the Event Dispatch Thread ───────────────
        SwingUtilities.invokeLater(LoginFrame::new);
    }
}
