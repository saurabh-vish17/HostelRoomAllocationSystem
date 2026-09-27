package com.hostel.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * SplashScreen — Phase 1 test window.
 *
 * This window confirms that:
 *   1. Java Swing is working correctly
 *   2. The Maven project structure compiles and runs
 *   3. Custom fonts, colors, and layout work on this machine
 *
 * This will be REPLACED by LoginFrame in Phase 5.
 * Do not add business logic here.
 */
public class SplashScreen {

    // ── Color Palette (matches the final app design) ──────────────────────────
    private static final Color COLOR_NAVY       = new Color(0x1e293b);
    private static final Color COLOR_BLUE       = new Color(0x3b82f6);
    private static final Color COLOR_BLUE_DARK  = new Color(0x1d4ed8);
    private static final Color COLOR_WHITE      = Color.WHITE;
    private static final Color COLOR_LIGHT_TEXT = new Color(0xe2e8f0);
    private static final Color COLOR_SUBTEXT    = new Color(0x94a3b8);
    private static final Color COLOR_SUCCESS    = new Color(0x22c55e);
    private static final Color COLOR_BG         = new Color(0x0f172a);

    private JFrame frame;

    /**
     * Builds and displays the splash / test window.
     */
    public void showSplash() {
        frame = new JFrame("Hostel Room Allocation System — Phase 1 Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(680, 480);
        frame.setLocationRelativeTo(null);    // center on screen
        frame.setResizable(false);

        // ── Root panel with dark background ───────────────────────────────────
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(COLOR_BG);
        root.setBorder(new EmptyBorder(40, 50, 40, 50));

        // ── Header section ────────────────────────────────────────────────────
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        // App icon (colored square as placeholder — Phase 5 can add a real icon)
        JLabel iconLabel = new JLabel("🏢");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("Hostel Room Allocation System");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(COLOR_WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("College Project — Java Swing + JDBC + MySQL");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(COLOR_SUBTEXT);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(iconLabel);
        header.add(Box.createVerticalStrut(10));
        header.add(titleLabel);
        header.add(Box.createVerticalStrut(6));
        header.add(subtitleLabel);

        // ── Status cards panel ────────────────────────────────────────────────
        JPanel cardsPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        cardsPanel.setOpaque(false);
        cardsPanel.setBorder(new EmptyBorder(30, 0, 30, 0));

        cardsPanel.add(makeStatusCard("✅  Java Swing",   "Working", COLOR_SUCCESS));
        cardsPanel.add(makeStatusCard("✅  Maven Build",  "Compiled", COLOR_SUCCESS));
        cardsPanel.add(makeStatusCard("✅  Project Structure", "Ready", COLOR_SUCCESS));
        cardsPanel.add(makeStatusCard("⏳  Database",    "Phase 2", COLOR_BLUE));

        // ── Phase info label ──────────────────────────────────────────────────
        JLabel phaseLabel = new JLabel("Phase 1 Complete  •  Ready to proceed to Phase 2");
        phaseLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        phaseLabel.setForeground(COLOR_SUBTEXT);
        phaseLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // ── Proceed button ────────────────────────────────────────────────────
        JButton proceedBtn = createStyledButton("Phase 1 Verified — Close");
        proceedBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        proceedBtn.addActionListener((ActionEvent e) -> {
            JOptionPane.showMessageDialog(
                frame,
                "✅  Phase 1 Verification Successful!\n\n" +
                "Java Swing is working.\n" +
                "Maven project compiled successfully.\n" +
                "Package structure is in place.\n\n" +
                "Proceed to Phase 2: Database Schema & Seed Data.",
                "Phase 1 Complete",
                JOptionPane.INFORMATION_MESSAGE
            );
            frame.dispose();
        });

        // ── Bottom panel ──────────────────────────────────────────────────────
        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setOpaque(false);
        bottom.add(phaseLabel);
        bottom.add(Box.createVerticalStrut(16));

        JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnWrapper.setOpaque(false);
        btnWrapper.add(proceedBtn);
        bottom.add(btnWrapper);

        // ── Assemble root ─────────────────────────────────────────────────────
        root.add(header,     BorderLayout.NORTH);
        root.add(cardsPanel, BorderLayout.CENTER);
        root.add(bottom,     BorderLayout.SOUTH);

        frame.setContentPane(root);
        frame.setVisible(true);
    }

    // ── Helper: status card ───────────────────────────────────────────────────

    /**
     * Creates a rounded-looking status card panel.
     * @param title     what is being checked
     * @param statusText the status label text
     * @param statusColor color for the status dot and text
     */
    private JPanel makeStatusCard(String title, String statusText, Color statusColor) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(COLOR_NAVY);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x334155), 1),
            new EmptyBorder(15, 18, 15, 18)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        titleLbl.setForeground(COLOR_LIGHT_TEXT);

        JLabel statusLbl = new JLabel(statusText);
        statusLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusLbl.setForeground(statusColor);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(statusLbl, BorderLayout.CENTER);

        return card;
    }

    // ── Helper: styled button ─────────────────────────────────────────────────

    /**
     * Creates a styled primary button matching the app design.
     */
    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(COLOR_BLUE_DARK);
                } else if (getModel().isRollover()) {
                    g2.setColor(COLOR_BLUE.brighter());
                } else {
                    g2.setColor(COLOR_BLUE);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(COLOR_WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(240, 40));
        btn.setOpaque(false);

        return btn;
    }
}
