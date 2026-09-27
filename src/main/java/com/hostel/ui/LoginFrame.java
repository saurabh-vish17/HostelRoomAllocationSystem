package com.hostel.ui;

import com.hostel.service.AuthException;
import com.hostel.service.AuthService;
import com.hostel.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * LoginFrame — the admin login screen.
 *
 * Design:
 *   - Dark navy theme matching the full application
 *   - Hospital/institutional clean look (not playful)
 *   - Icon + title + subtitle header
 *   - Username and password fields with focus-highlight border
 *   - Primary "Sign In" button + secondary "Clear" button
 *   - Red error label that only appears on failure
 *   - Enter key logs in from either field
 *
 * Flow:
 *   Opens on: application start (Main.java)
 *   Closes on: successful login (opens DashboardFrame)
 */
public class LoginFrame extends JFrame {

    // ── Colour palette ─────────────────────────────────────────────────────
    private static final Color BG_MAIN      = new Color(0x0f172a);  // very dark navy
    private static final Color BG_CARD      = new Color(0x1e293b);  // card background
    private static final Color BG_INPUT     = new Color(0x0f172a);  // input background
    private static final Color BORDER_IDLE  = new Color(0x334155);  // unfocused border
    private static final Color BORDER_FOCUS = new Color(0x3b82f6);  // focused (blue)
    private static final Color TEXT_WHITE   = new Color(0xf1f5f9);  // main text
    private static final Color TEXT_LABEL   = new Color(0x94a3b8);  // field labels
    private static final Color TEXT_MUTED   = new Color(0x475569);  // footer
    private static final Color BTN_PRIMARY  = new Color(0x3b82f6);  // blue button
    private static final Color BTN_HOVER    = new Color(0x2563eb);  // darker on hover
    private static final Color BTN_2ND      = new Color(0x1e293b);  // secondary button bg
    private static final Color BTN_2ND_HOVER= new Color(0x334155);
    private static final Color BTN_2ND_BDR  = new Color(0x334155);  // secondary button border
    private static final Color COLOR_ERROR  = new Color(0xef4444);  // error red
    private static final Color ACCENT_LINE  = new Color(0x3b82f6);  // header underline

    // ── Fonts ──────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_SUB    = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_LABEL  = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_INPUT  = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_BTN    = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 12);

    // ── Component references ───────────────────────────────────────────────
    private JTextField     usernameField;
    private JPasswordField passwordField;
    private JButton        loginButton;
    private JButton        clearButton;
    private JLabel         errorLabel;
    private JLabel         statusLabel;

    // ── Service ────────────────────────────────────────────────────────────
    private final AuthService authService = new AuthService();

    // ── Constructor ────────────────────────────────────────────────────────

    public LoginFrame() {
        initWindow();
        buildUI();
        wireListeners();
        pack();
        setSize(460, 580);
        setLocationRelativeTo(null);    // centre on screen
        setVisible(true);
        usernameField.requestFocusInWindow();
    }

    // ── Window setup ───────────────────────────────────────────────────────

    private void initWindow() {
        setTitle("Hostel Room Allocation System — Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        // Dark title-bar background only on Windows 11 (ignored gracefully on others)
        getRootPane().putClientProperty("JRootPane.titleBarBackground", BG_MAIN);
        getRootPane().putClientProperty("JRootPane.titleBarForeground", Color.WHITE);
    }

    // ── UI construction ────────────────────────────────────────────────────

    private void buildUI() {
        // Root panel fills entire window
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_MAIN);
        root.setBorder(new EmptyBorder(40, 48, 28, 48));
        setContentPane(root);

        root.add(buildHeader(),  BorderLayout.NORTH);
        root.add(buildCard(),    BorderLayout.CENTER);
        root.add(buildFooter(),  BorderLayout.SOUTH);
    }

    // ── Header ─────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 32, 0));

        // Emoji icon (renders on all modern JVMs)
        JLabel icon = new JLabel("🏢", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 44));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Hostel Management System", SwingConstants.CENTER);
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Coloured accent line under title
        JPanel accentLine = new JPanel();
        accentLine.setBackground(ACCENT_LINE);
        accentLine.setPreferredSize(new Dimension(60, 3));
        accentLine.setMaximumSize(new Dimension(60, 3));
        accentLine.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Room Allocation & Management", SwingConstants.CENTER);
        sub.setFont(FONT_SUB);
        sub.setForeground(TEXT_LABEL);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(icon);
        header.add(Box.createVerticalStrut(10));
        header.add(title);
        header.add(Box.createVerticalStrut(8));
        header.add(accentLine);
        header.add(Box.createVerticalStrut(8));
        header.add(sub);
        return header;
    }

    // ── Form card ──────────────────────────────────────────────────────────

    private JPanel buildCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x334155), 1),
            new EmptyBorder(30, 28, 28, 28)
        ));

        // ── Username ──────────────────────────────────────────────────────
        card.add(makeLabel("Username"));
        card.add(Box.createVerticalStrut(7));
        usernameField = makeTextField();
        usernameField.setToolTipText("Enter your admin username");
        card.add(usernameField);

        card.add(Box.createVerticalStrut(20));

        // ── Password ──────────────────────────────────────────────────────
        card.add(makeLabel("Password"));
        card.add(Box.createVerticalStrut(7));
        passwordField = makePasswordField();
        passwordField.setToolTipText("Enter your password");
        card.add(passwordField);

        card.add(Box.createVerticalStrut(28));

        // ── Login button ──────────────────────────────────────────────────
        loginButton = makePrimaryButton("Sign In");
        card.add(loginButton);

        card.add(Box.createVerticalStrut(10));

        // ── Clear button ──────────────────────────────────────────────────
        clearButton = makeSecondaryButton("Clear Fields");
        card.add(clearButton);

        card.add(Box.createVerticalStrut(18));

        // ── Error label ────────────────────────────────────────────────────
        errorLabel = new JLabel(" ", SwingConstants.CENTER);
        errorLabel.setFont(FONT_SMALL);
        errorLabel.setForeground(COLOR_ERROR);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(errorLabel);

        return card;
    }

    // ── Footer ─────────────────────────────────────────────────────────────

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(16, 0, 0, 0));

        // Status label (left)
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(TEXT_MUTED);

        // Version (right)
        JLabel version = new JLabel("v1.0.0  |  Phase 4");
        version.setFont(FONT_SMALL);
        version.setForeground(TEXT_MUTED);

        footer.add(statusLabel, BorderLayout.WEST);
        footer.add(version,     BorderLayout.EAST);
        return footer;
    }

    // ── Event wiring ───────────────────────────────────────────────────────

    private void wireListeners() {
        // Primary action
        loginButton.addActionListener(e -> performLogin());

        // Clear action
        clearButton.addActionListener(e -> clearFields());

        // Enter key on username → move to password
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());

        // Enter key on password → attempt login
        passwordField.addActionListener(e -> performLogin());

        // Focus borders on username
        usernameField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                usernameField.setBorder(inputBorderFocused());
                clearError();
            }
            @Override public void focusLost(FocusEvent e) {
                usernameField.setBorder(inputBorderIdle());
            }
        });

        // Focus borders on password
        passwordField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                passwordField.setBorder(inputBorderFocused());
                clearError();
            }
            @Override public void focusLost(FocusEvent e) {
                passwordField.setBorder(inputBorderIdle());
            }
        });
    }

    // ── Login action ───────────────────────────────────────────────────────

    /**
     * Attempts login with the current field values.
     * Disables buttons during the attempt to prevent double-clicks.
     */
    private void performLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        setFormEnabled(false);
        setStatus("Signing in…");
        clearError();

        // Run on background thread so the UI doesn't freeze during DB call
        SwingWorker<User, Void> worker = new SwingWorker<>() {
            @Override
            protected User doInBackground() throws Exception {
                return authService.login(username, password);
            }

            @Override
            protected void done() {
                try {
                    User user = get();                     // throws if doInBackground threw
                    onLoginSuccess(user);
                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof AuthException) {
                        showError(cause.getMessage());
                    } else {
                        showError("Unexpected error: " + cause.getMessage());
                    }
                    setFormEnabled(true);
                    setStatus("Ready");
                    passwordField.setText("");
                    passwordField.requestFocusInWindow();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        worker.execute();
    }

    // ── Post-login ─────────────────────────────────────────────────────────

    private void onLoginSuccess(User user) {
        setStatus("Welcome, " + user.getFullName() + "!");

        // Brief pause so the user can see the success status, then open dashboard
        Timer timer = new Timer(600, e -> {
            SwingUtilities.invokeLater(() -> {
                new DashboardFrame();
                dispose();   // close LoginFrame
            });
        });
        timer.setRepeats(false);
        timer.start();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        clearError();
        usernameField.requestFocusInWindow();
        setStatus("Fields cleared");
    }

    private void showError(String msg) {
        // Replace newlines with HTML line breaks so multi-line fits in one label
        String html = "<html><center>" + msg.replace("\n", "<br>") + "</center></html>";
        errorLabel.setText(html);
        // Shake the card to signal error
        shakeWindow();
    }

    private void clearError() {
        errorLabel.setText(" ");
    }

    private void setStatus(String msg) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(msg));
    }

    private void setFormEnabled(boolean enabled) {
        usernameField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        loginButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
    }

    /**
     * Briefly shakes the window left-right to signal invalid credentials.
     */
    private void shakeWindow() {
        Point origin = getLocation();
        int[] xDeltas = {10, -10, 8, -8, 5, -5, 0};
        Timer shaker = new Timer(40, null);
        int[] step = {0};
        shaker.addActionListener(e -> {
            if (step[0] >= xDeltas.length) {
                setLocation(origin);
                shaker.stop();
            } else {
                setLocation(origin.x + xDeltas[step[0]], origin.y);
                step[0]++;
            }
        });
        shaker.start();
    }

    // ── Factory helpers ────────────────────────────────────────────────────

    private JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXT_LABEL);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JTextField makeTextField() {
        JTextField field = new JTextField();
        field.setFont(FONT_INPUT);
        field.setBackground(BG_INPUT);
        field.setForeground(TEXT_WHITE);
        field.setCaretColor(new Color(0x3b82f6));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setBorder(inputBorderIdle());
        field.setSelectionColor(new Color(0x3b82f6));
        field.setSelectedTextColor(Color.WHITE);
        return field;
    }

    private JPasswordField makePasswordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(FONT_INPUT);
        field.setBackground(BG_INPUT);
        field.setForeground(TEXT_WHITE);
        field.setCaretColor(new Color(0x3b82f6));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setBorder(inputBorderIdle());
        field.setSelectionColor(new Color(0x3b82f6));
        field.setSelectedTextColor(Color.WHITE);
        field.setEchoChar('●');
        return field;
    }

    private JButton makePrimaryButton(String text) {
        JButton btn = new JButton(text) {
            private boolean hover = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                    public void mouseExited (MouseEvent e) { hover = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = !isEnabled() ? new Color(0x1e3a5f)
                         : hover        ? BTN_HOVER
                         :               BTN_PRIMARY;
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BTN);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setOpaque(false);
        return btn;
    }

    private JButton makeSecondaryButton(String text) {
        JButton btn = new JButton(text) {
            private boolean hover = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                    public void mouseExited (MouseEvent e) { hover = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hover ? BTN_2ND_HOVER : BTN_2ND);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(BTN_2ND_BDR);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-1, getHeight()-1, 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BTN);
        btn.setForeground(new Color(0x94a3b8));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setOpaque(false);
        return btn;
    }

    private javax.swing.border.Border inputBorderIdle() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_IDLE, 1),
            new EmptyBorder(9, 13, 9, 13)
        );
    }

    private javax.swing.border.Border inputBorderFocused() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_FOCUS, 2),
            new EmptyBorder(8, 12, 8, 12)
        );
    }
}
