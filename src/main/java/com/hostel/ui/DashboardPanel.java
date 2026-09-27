package com.hostel.ui;

import com.hostel.model.DashboardStats;
import com.hostel.model.DashboardStats.BlockStats;
import com.hostel.service.DashboardService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * DashboardPanel — Dynamic Swing panel displaying overall hostel statistics
 * and block-wise breakdowns (D, G, H) loaded live from MySQL.
 */
public class DashboardPanel extends JPanel {

    // ── Palette ────────────────────────────────────────────────────────────
    private static final Color BG_CANVAS     = new Color(0xf8fafc); // slate 50
    private static final Color CARD_BG       = Color.WHITE;
    private static final Color CARD_BORDER   = new Color(0xe2e8f0); // slate 200
    private static final Color TEXT_DARK     = new Color(0x0f172a); // slate 900
    private static final Color TEXT_MUTED    = new Color(0x64748b); // slate 500
    private static final Color ACCENT_BLUE   = new Color(0x3b82f6); // blue 500
    private static final Color ACCENT_GREEN  = new Color(0x10b981); // emerald 500
    private static final Color ACCENT_PURPLE = new Color(0x8b5cf6); // violet 500
    private static final Color ACCENT_AMBER  = new Color(0xf59e0b); // amber 500
    private static final Color ACCENT_RED    = new Color(0xef4444); // red 500
    private static final Color ACCENT_CYAN   = new Color(0x06b6d4); // cyan 500
    private static final Color ACCENT_INDIGO = new Color(0x6366f1); // indigo 500
    private static final Color ACCENT_SLATE  = new Color(0x475569); // slate 600

    // ── Components to update dynamically ──────────────────────────────────
    private JLabel lblTotalStudents;
    private JLabel lblTotalRooms;
    private JLabel lblTotalCapacity;
    private JLabel lblOccupiedBeds;
    private JLabel lblAvailableBeds;
    private JLabel lblAvailableRooms;
    private JLabel lblFullRooms;
    private JLabel lblPartialRooms;
    private JLabel lblLastUpdated;
    private JButton btnRefresh;

    // Block D Labels & Bar
    private JLabel lblBlockDRooms, lblBlockDCap, lblBlockDOcc, lblBlockDAvail, lblBlockDStatus;
    private JProgressBar barBlockD;

    // Block G Labels & Bar
    private JLabel lblBlockGRooms, lblBlockGCap, lblBlockGOcc, lblBlockGAvail, lblBlockGStatus;
    private JProgressBar barBlockG;

    // Block H Labels & Bar
    private JLabel lblBlockHRooms, lblBlockHCap, lblBlockHOcc, lblBlockHAvail, lblBlockHStatus;
    private JProgressBar barBlockH;

    private final DashboardService dashboardService = new DashboardService();

    public DashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(BG_CANVAS);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        // Use scroll pane to handle smaller resolutions cleanly
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(BG_CANVAS);

        container.add(buildHeader());
        container.add(Box.createVerticalStrut(20));
        container.add(buildTopStatsGrid());
        container.add(Box.createVerticalStrut(24));
        container.add(buildBlockBreakdownSection());

        JScrollPane scrollPane = new JScrollPane(container);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(BG_CANVAS);
        add(scrollPane, BorderLayout.CENTER);

        // Load data on panel initialization
        refreshData();
    }

    // ── Header ─────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel title = new JLabel("Dashboard Overview");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_DARK);

        JLabel sub = new JLabel("Live hostel capacity, occupancy, and room allocation statistics");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(TEXT_MUTED);

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(sub);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        lblLastUpdated = new JLabel("Updated: Just now");
        lblLastUpdated.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblLastUpdated.setForeground(TEXT_MUTED);

        btnRefresh = makeButton("🔄  Refresh Data", ACCENT_BLUE);
        btnRefresh.addActionListener(e -> refreshData());

        right.add(lblLastUpdated);
        right.add(btnRefresh);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    // ── Overall Stats Grid (8 cards) ───────────────────────────────────────

    private JPanel buildTopStatsGrid() {
        JPanel grid = new JPanel(new GridLayout(2, 4, 16, 16));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 210));

        // Initialize value labels
        lblTotalStudents  = new JLabel("-", SwingConstants.LEFT);
        lblTotalRooms     = new JLabel("-", SwingConstants.LEFT);
        lblTotalCapacity  = new JLabel("-", SwingConstants.LEFT);
        lblOccupiedBeds   = new JLabel("-", SwingConstants.LEFT);
        lblAvailableBeds  = new JLabel("-", SwingConstants.LEFT);
        lblAvailableRooms = new JLabel("-", SwingConstants.LEFT);
        lblFullRooms      = new JLabel("-", SwingConstants.LEFT);
        lblPartialRooms   = new JLabel("-", SwingConstants.LEFT);

        grid.add(createStatCard("Total Students",     lblTotalStudents,  "🎓", ACCENT_BLUE));
        grid.add(createStatCard("Total Rooms",        lblTotalRooms,     "🚪", ACCENT_INDIGO));
        grid.add(createStatCard("Total Bed Capacity", lblTotalCapacity,  "🛏️", ACCENT_PURPLE));
        grid.add(createStatCard("Occupied Beds",      lblOccupiedBeds,   "👤", ACCENT_AMBER));
        grid.add(createStatCard("Available Beds",     lblAvailableBeds,  "✅", ACCENT_GREEN));
        grid.add(createStatCard("Available Rooms",    lblAvailableRooms, "🔓", ACCENT_CYAN));
        grid.add(createStatCard("Full Rooms",         lblFullRooms,      "🛑", ACCENT_RED));
        grid.add(createStatCard("Partial Rooms",      lblPartialRooms,   "🌓", ACCENT_SLATE));

        return grid;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, String iconStr, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 1),
            new EmptyBorder(14, 16, 14, 16)
        ));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLbl.setForeground(TEXT_MUTED);

        JLabel iconLbl = new JLabel(iconStr);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));

        topRow.add(titleLbl, BorderLayout.WEST);
        topRow.add(iconLbl, BorderLayout.EAST);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(accentColor);

        card.add(topRow, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    // ── Block Breakdown Section ────────────────────────────────────────────

    private JPanel buildBlockBreakdownSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);

        JLabel secTitle = new JLabel("Block Infrastructure & Occupancy");
        secTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        secTitle.setForeground(TEXT_DARK);
        secTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        section.add(secTitle);
        section.add(Box.createVerticalStrut(14));

        JPanel cardsRow = new JPanel(new GridLayout(1, 3, 16, 0));
        cardsRow.setOpaque(false);
        cardsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        // Block D Card
        cardsRow.add(buildBlockCard("D Block", "5 Floors  ●  12 Rooms/Floor", ACCENT_BLUE,
            lblBlockDRooms = new JLabel("-"),
            lblBlockDCap   = new JLabel("-"),
            lblBlockDOcc   = new JLabel("-"),
            lblBlockDAvail = new JLabel("-"),
            lblBlockDStatus= new JLabel("-"),
            barBlockD      = new JProgressBar(0, 100)));

        // Block G Card
        cardsRow.add(buildBlockCard("G Block", "4 Floors  ●  53 Rooms/Floor", ACCENT_PURPLE,
            lblBlockGRooms = new JLabel("-"),
            lblBlockGCap   = new JLabel("-"),
            lblBlockGOcc   = new JLabel("-"),
            lblBlockGAvail = new JLabel("-"),
            lblBlockGStatus= new JLabel("-"),
            barBlockG      = new JProgressBar(0, 100)));

        // Block H Card
        cardsRow.add(buildBlockCard("H Block", "6 Floors  ●  30 Rooms/Floor", ACCENT_GREEN,
            lblBlockHRooms = new JLabel("-"),
            lblBlockHCap   = new JLabel("-"),
            lblBlockHOcc   = new JLabel("-"),
            lblBlockHAvail = new JLabel("-"),
            lblBlockHStatus= new JLabel("-"),
            barBlockH      = new JProgressBar(0, 100)));

        cardsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(cardsRow);

        return section;
    }

    private JPanel buildBlockCard(String blockName, String subtitle, Color colorTheme,
                                  JLabel lblRooms, JLabel lblCap, JLabel lblOcc,
                                  JLabel lblAvail, JLabel lblStatus, JProgressBar progressBar) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 1),
            new EmptyBorder(16, 18, 16, 18)
        ));

        // Card Header
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);

        JLabel title = new JLabel(blockName);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(colorTheme);

        JLabel sub = new JLabel(subtitle);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(TEXT_MUTED);

        head.add(title, BorderLayout.NORTH);
        head.add(sub, BorderLayout.SOUTH);
        card.add(head);

        card.add(Box.createVerticalStrut(12));

        // Progress Bar
        progressBar.setStringPainted(true);
        progressBar.setFont(new Font("Segoe UI", Font.BOLD, 11));
        progressBar.setForeground(colorTheme);
        progressBar.setBackground(new Color(0xf1f5f9));
        progressBar.setBorder(BorderFactory.createEmptyBorder());
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        card.add(progressBar);

        card.add(Box.createVerticalStrut(14));

        // Details grid
        JPanel details = new JPanel(new GridLayout(4, 2, 8, 6));
        details.setOpaque(false);

        addDetailRow(details, "Total Rooms:", lblRooms);
        addDetailRow(details, "Bed Capacity:", lblCap);
        addDetailRow(details, "Occupied Beds:", lblOcc);
        addDetailRow(details, "Available Beds:", lblAvail);

        card.add(details);
        card.add(Box.createVerticalStrut(10));

        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblStatus.setForeground(TEXT_MUTED);
        card.add(lblStatus);

        return card;
    }

    private void addDetailRow(JPanel parent, String labelText, JLabel valueLabel) {
        JLabel k = new JLabel(labelText);
        k.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        k.setForeground(TEXT_MUTED);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        valueLabel.setForeground(TEXT_DARK);

        parent.add(k);
        parent.add(valueLabel);
    }

    // ── Data Refresh Logic ─────────────────────────────────────────────────

    public void refreshData() {
        btnRefresh.setEnabled(false);
        btnRefresh.setText("Refreshing...");

        SwingWorker<DashboardStats, Void> worker = new SwingWorker<>() {
            @Override
            protected DashboardStats doInBackground() throws Exception {
                return dashboardService.getDashboardStatistics();
            }

            @Override
            protected void done() {
                try {
                    DashboardStats stats = get();
                    updateUIWithStats(stats);
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a");
                    lblLastUpdated.setText("Updated: " + sdf.format(new Date()));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                        DashboardPanel.this,
                        "Failed to load dashboard statistics from MySQL:\n" + ex.getMessage(),
                        "Database Query Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                } finally {
                    btnRefresh.setEnabled(true);
                    btnRefresh.setText("🔄  Refresh Data");
                }
            }
        };
        worker.execute();
    }

    private void updateUIWithStats(DashboardStats stats) {
        // Overall cards
        lblTotalStudents.setText(String.format("%,d", stats.getTotalStudents()));
        lblTotalRooms.setText(String.format("%,d", stats.getTotalRooms()));
        lblTotalCapacity.setText(String.format("%,d", stats.getTotalCapacity()));
        lblOccupiedBeds.setText(String.format("%,d", stats.getOccupiedBeds()));
        lblAvailableBeds.setText(String.format("%,d", stats.getAvailableBeds()));
        lblAvailableRooms.setText(String.format("%,d", stats.getAvailableRooms()));
        lblFullRooms.setText(String.format("%,d", stats.getFullRooms()));
        lblPartialRooms.setText(String.format("%,d", stats.getPartiallyOccupiedRooms()));

        // Block D
        updateBlockCard(stats.getBlockD(), lblBlockDRooms, lblBlockDCap, lblBlockDOcc,
            lblBlockDAvail, lblBlockDStatus, barBlockD);

        // Block G
        updateBlockCard(stats.getBlockG(), lblBlockGRooms, lblBlockGCap, lblBlockGOcc,
            lblBlockGAvail, lblBlockGStatus, barBlockG);

        // Block H
        updateBlockCard(stats.getBlockH(), lblBlockHRooms, lblBlockHCap, lblBlockHOcc,
            lblBlockHAvail, lblBlockHStatus, barBlockH);
    }

    private void updateBlockCard(BlockStats bStats, JLabel lblRooms, JLabel lblCap,
                                 JLabel lblOcc, JLabel lblAvail, JLabel lblStatus,
                                 JProgressBar bar) {
        if (bStats == null) return;

        lblRooms.setText(String.valueOf(bStats.getTotalRooms()));
        lblCap.setText(String.valueOf(bStats.getTotalCapacity()));
        lblOcc.setText(String.valueOf(bStats.getOccupiedBeds()));
        lblAvail.setText(String.valueOf(bStats.getAvailableBeds()));

        int pct = (int) Math.round(bStats.getOccupancyPercentage());
        bar.setValue(pct);
        bar.setString(pct + "% Occupied (" + bStats.getOccupiedBeds() + "/" + bStats.getTotalCapacity() + ")");

        lblStatus.setText(String.format(
            "Rooms: %d Avail  ●  %d Partial  ●  %d Full",
            bStats.getAvailableRooms(), bStats.getPartialRooms(), bStats.getFullRooms()
        ));
    }

    private JButton makeButton(String text, Color bg) {
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
                g2.setColor(hover ? bg.darker() : bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(135, 34));
        btn.setOpaque(false);
        return btn;
    }
}
