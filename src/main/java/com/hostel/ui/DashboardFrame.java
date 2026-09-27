package com.hostel.ui;

import com.hostel.service.AuthService;
import com.hostel.util.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * DashboardFrame — main application window after login.
 *
 * Structure:
 *   ┌────────────────────────────────────────────────────────┐
 *   │  TOP BAR  — app icon + title + user info + logout      │
 *   ├────────────┬───────────────────────────────────────────┤
 *   │  SIDEBAR   │  CONTENT AREA  (CardLayout)               │
 *   │            │                                           │
 *   │ [Dash]     │  Dynamic DashboardPanel (live MySQL stats)│
 *   │ [Students] │  Coming Soon / Feature panels            │
 *   │ [Blocks]   │                                           │
 *   │ [Rooms]    │                                           │
 *   │ [Allocate] │                                           │
 *   │ [Vacate]   │                                           │
 *   │ [Reports]  │                                           │
 *   │            │                                           │
 *   │ [Logout]   │                                           │
 *   └────────────┴───────────────────────────────────────────┘
 */
public class DashboardFrame extends JFrame {

    // ── Colours ────────────────────────────────────────────────────────────
    private static final Color SIDEBAR_BG      = new Color(0x1e293b);
    private static final Color SIDEBAR_HOVER   = new Color(0x293548);
    private static final Color SIDEBAR_ACTIVE  = new Color(0x3b82f6);
    private static final Color SIDEBAR_TEXT    = new Color(0xcbd5e1);
    private static final Color SIDEBAR_MUTED   = new Color(0x64748b);
    private static final Color TOPBAR_BG       = new Color(0x1e293b);
    private static final Color TOPBAR_BORDER   = new Color(0x293548);
    private static final Color CONTENT_BG      = new Color(0xf8fafc);
    private static final Color TEXT_WHITE      = new Color(0xf1f5f9);
    private static final Color TEXT_SOFT       = new Color(0x94a3b8);
    private static final Color DANGER_RED      = new Color(0xef4444);
    private static final Color DANGER_HOVER    = new Color(0xdc2626);

    // ── Fonts ──────────────────────────────────────────────────────────────
    private static final Font FONT_APP_TITLE = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font FONT_NAV       = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_NAV_HEAD  = new Font("Segoe UI", Font.BOLD, 11);
    private static final Font FONT_USER_NAME = new Font("Segoe UI", Font.BOLD, 13);

    // ── Components ─────────────────────────────────────────────────────────
    private final CardLayout     cardLayout     = new CardLayout();
    private final JPanel         contentArea    = new JPanel(cardLayout);
    private       JButton        activeNavBtn   = null;
    private       DashboardPanel dashboardPanel;
    private       ReportsPanel   reportsPanel;

    // ── Panel key constants (used to switch cards) ─────────────────────────
    public static final String PANEL_DASHBOARD  = "dashboard";
    public static final String PANEL_STUDENTS   = "students";
    public static final String PANEL_BLOCKS     = "blocks";
    public static final String PANEL_ROOMS      = "rooms";
    public static final String PANEL_ALLOCATION = "allocation";
    public static final String PANEL_VACATE     = "vacate";
    public static final String PANEL_REPORTS    = "reports";

    // ── Constructor ────────────────────────────────────────────────────────

    public DashboardFrame() {
        // Safety guard: redirect to login if no session
        if (!SessionManager.isLoggedIn()) {
            new LoginFrame();
            dispose();
            return;
        }

        initWindow();
        buildUI();
        setVisible(true);

        showPanel(PANEL_DASHBOARD);
    }

    // ── Window ─────────────────────────────────────────────────────────────

    private void initWindow() {
        setTitle("Hostel Room Allocation System  —  " + SessionManager.getFullName());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1180, 740);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);

        // Confirm before closing via window X button
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                confirmLogout();
            }
        });
    }

    // ── UI assembly ────────────────────────────────────────────────────────

    private void buildUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(CONTENT_BG);

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        add(buildContent(), BorderLayout.CENTER);
    }

    // ── Top bar ────────────────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(TOPBAR_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, TOPBAR_BORDER),
            new EmptyBorder(12, 20, 12, 20)
        ));
        bar.setPreferredSize(new Dimension(0, 56));

        // Left: icon + title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        JLabel icon = new JLabel("🏢");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));

        JLabel appTitle = new JLabel("Hostel Room Allocation System");
        appTitle.setFont(FONT_APP_TITLE);
        appTitle.setForeground(TEXT_WHITE);

        JLabel separator = new JLabel(" | ");
        separator.setForeground(new Color(0x334155));

        JLabel phase = new JLabel("Admin Dashboard");
        phase.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        phase.setForeground(TEXT_SOFT);

        left.add(icon);
        left.add(appTitle);
        left.add(separator);
        left.add(phase);

        // Right: user info + logout button
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        JPanel userInfo = new JPanel();
        userInfo.setLayout(new BoxLayout(userInfo, BoxLayout.Y_AXIS));
        userInfo.setOpaque(false);

        JLabel userName = new JLabel(SessionManager.getFullName());
        userName.setFont(FONT_USER_NAME);
        userName.setForeground(TEXT_WHITE);
        userName.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel userRole = new JLabel("Administrator  ●  " + SessionManager.getUsername());
        userRole.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        userRole.setForeground(TEXT_SOFT);
        userRole.setAlignmentX(Component.RIGHT_ALIGNMENT);

        userInfo.add(userName);
        userInfo.add(userRole);

        JButton logoutTopBtn = makeLogoutButton("⏻  Logout");
        logoutTopBtn.addActionListener(e -> confirmLogout());

        right.add(userInfo);
        right.add(logoutTopBtn);

        bar.add(left,  BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ── Sidebar ────────────────────────────────────────────────────────────

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 0, 1, new Color(0x293548)),
            new EmptyBorder(16, 0, 16, 0)
        ));

        // Section: MAIN
        sidebar.add(navSectionLabel("MAIN"));
        JButton dashBtn = makeNavButton("  📊  Dashboard",  PANEL_DASHBOARD);
        activeNavBtn = dashBtn;
        setActiveStyle(dashBtn);
        sidebar.add(dashBtn);

        // Section: MANAGEMENT
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(navSectionLabel("MANAGEMENT"));
        sidebar.add(makeNavButton("  👤  Students",   PANEL_STUDENTS));
        sidebar.add(makeNavButton("  🏢  Blocks",     PANEL_BLOCKS));
        sidebar.add(makeNavButton("  🚪  Rooms",      PANEL_ROOMS));

        // Section: OPERATIONS
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(navSectionLabel("OPERATIONS"));
        sidebar.add(makeNavButton("  📝  Allocation", PANEL_ALLOCATION));
        sidebar.add(makeNavButton("  🚶  Vacate",     PANEL_VACATE));
        sidebar.add(makeNavButton("  📈  Reports",    PANEL_REPORTS));

        // Push logout to bottom
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(new JSeparator() {{
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            setForeground(new Color(0x293548));
        }});
        sidebar.add(Box.createVerticalStrut(8));

        JButton logoutSideBtn = makeNavButton("  ⏻   Logout", "logout");
        logoutSideBtn.setForeground(new Color(0xfca5a5));
        logoutSideBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { logoutSideBtn.setBackground(new Color(0x3d1515)); logoutSideBtn.repaint(); }
            public void mouseExited (MouseEvent e) { logoutSideBtn.setBackground(SIDEBAR_BG); logoutSideBtn.repaint(); }
        });
        logoutSideBtn.addActionListener(e -> confirmLogout());
        sidebar.add(logoutSideBtn);
        sidebar.add(Box.createVerticalStrut(8));

        return sidebar;
    }

    // ── Content area ───────────────────────────────────────────────────────

    private JPanel buildContent() {
        contentArea.setBackground(CONTENT_BG);

        // Dynamic Dashboard panel
        dashboardPanel = new DashboardPanel();
        contentArea.add(dashboardPanel, PANEL_DASHBOARD);

        // Student Management Panel
        contentArea.add(new StudentPanel(), PANEL_STUDENTS);
        // Block Overview & Management Panel
        contentArea.add(new BlockPanel(), PANEL_BLOCKS);
        // Room Management Panel
        contentArea.add(new RoomPanel(), PANEL_ROOMS);
        // Room Allocation Panel
        contentArea.add(new AllocationPanel(), PANEL_ALLOCATION);
        // Room Vacating Panel
        reportsPanel = new ReportsPanel();
        contentArea.add(reportsPanel, PANEL_REPORTS);

        return contentArea;
    }

    // ── Coming Soon panel ──────────────────────────────────────────────────

    private JPanel buildComingSoonPanel(String title, String phase) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(CONTENT_BG);
        panel.setBorder(new EmptyBorder(80, 80, 80, 80));

        JLabel icon = new JLabel("🚧", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLbl.setForeground(new Color(0x1e293b));
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel coming = new JLabel("Coming in " + phase);
        coming.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        coming.setForeground(new Color(0x64748b));
        coming.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(icon);
        panel.add(Box.createVerticalStrut(20));
        panel.add(titleLbl);
        panel.add(Box.createVerticalStrut(10));
        panel.add(coming);
        return panel;
    }

    // ── Navigation ─────────────────────────────────────────────────────────

    /**
     * Shows the named card in the content area and updates sidebar highlight.
     */
    public void showPanel(String panelName) {
        cardLayout.show(contentArea, panelName);
        if (PANEL_DASHBOARD.equals(panelName) && dashboardPanel != null) {
            dashboardPanel.refreshData();
        } else if (PANEL_REPORTS.equals(panelName) && reportsPanel != null) {
            reportsPanel.refreshData();
        }
    }

    // ── Logout ─────────────────────────────────────────────────────────────

    private void confirmLogout() {
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to log out?",
            "Confirm Logout",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        if (choice == JOptionPane.YES_OPTION) {
            performLogout();
        }
    }

    private void performLogout() {
        new AuthService().logout();
        SwingUtilities.invokeLater(() -> {
            new LoginFrame();
            dispose();
        });
    }

    // ── Factory helpers ────────────────────────────────────────────────────

    private JButton makeNavButton(String label, String panelKey) {
        JButton btn = new JButton(label) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_NAV);
        btn.setForeground(SIDEBAR_TEXT);
        btn.setBackground(SIDEBAR_BG);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setMinimumSize(new Dimension(210, 42));
        btn.setPreferredSize(new Dimension(210, 42));
        btn.setBorder(new EmptyBorder(0, 16, 0, 16));
        btn.setOpaque(true);

        // Only wire panel switching for non-logout buttons
        if (!"logout".equals(panelKey)) {
            btn.addActionListener(e -> {
                showPanel(panelKey);
                updateActiveButton(btn);
            });
            btn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    if (btn != activeNavBtn) {
                        btn.setBackground(SIDEBAR_HOVER);
                    }
                }
                public void mouseExited(MouseEvent e) {
                    if (btn != activeNavBtn) {
                        btn.setBackground(SIDEBAR_BG);
                    }
                }
            });
        }
        return btn;
    }

    private void updateActiveButton(JButton clicked) {
        if (activeNavBtn != null && activeNavBtn != clicked) {
            activeNavBtn.setBackground(SIDEBAR_BG);
            activeNavBtn.setForeground(SIDEBAR_TEXT);
        }
        activeNavBtn = clicked;
        setActiveStyle(clicked);
    }

    private void setActiveStyle(JButton btn) {
        btn.setBackground(SIDEBAR_ACTIVE);
        btn.setForeground(Color.WHITE);
    }

    private JLabel navSectionLabel(String text) {
        JLabel lbl = new JLabel("  " + text);
        lbl.setFont(FONT_NAV_HEAD);
        lbl.setForeground(SIDEBAR_MUTED);
        lbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        lbl.setBorder(new EmptyBorder(0, 6, 0, 6));
        return lbl;
    }

    private JButton makeLogoutButton(String label) {
        JButton btn = new JButton(label) {
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
                g2.setColor(hover ? DANGER_HOVER : DANGER_RED);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 6, 6));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, 34));
        btn.setOpaque(false);
        return btn;
    }
}
