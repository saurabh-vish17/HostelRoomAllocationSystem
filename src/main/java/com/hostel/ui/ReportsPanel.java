package com.hostel.ui;

import com.hostel.model.AllocationHistoryStats;
import com.hostel.model.DashboardStats;
import com.hostel.model.DashboardStats.BlockStats;
import com.hostel.model.FloorStats;
import com.hostel.service.ReportService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * ReportsPanel — Comprehensive Reports & Analytics Module for Hostel Management.
 *
 * Provides:
 *  1. Overall System Statistics (Total Students, Rooms, Capacity, Beds, Room Status breakdown)
 *  2. Block-Wise Statistics (D, G, H)
 *  3. Floor-Wise Breakdown Table with dynamic filtering
 *  4. Allocation History Analytics (Active vs Vacated vs Total)
 *  5. Custom antialiased Java 2D Charts (Block Occupancy Bar Chart & Allocation Donut Chart)
 *
 * All data is computed dynamically from MySQL. No hard-coded values.
 */
public class ReportsPanel extends JPanel {

    // Palette Colors
    private static final Color BG_MAIN       = new Color(248, 250, 252);
    private static final Color CARD_BG       = Color.WHITE;
    private static final Color TEXT_DARK     = new Color(15, 23, 42);
    private static final Color TEXT_MUTED    = new Color(100, 116, 139);
    private static final Color BORDER_COLOR  = new Color(226, 232, 240);

    private static final Color COLOR_BLUE    = new Color(37, 99, 235);
    private static final Color COLOR_GREEN   = new Color(16, 185, 129);
    private static final Color COLOR_AMBER   = new Color(245, 158, 11);
    private static final Color COLOR_RED     = new Color(239, 68, 68);
    private static final Color COLOR_PURPLE  = new Color(147, 51, 234);

    private final ReportService reportService;

    // UI Controls for Live Updating
    private JLabel lblTotalStudents;
    private JLabel lblTotalRooms;
    private JLabel lblTotalCapacity;
    private JLabel lblOccupiedBeds;
    private JLabel lblAvailableBeds;
    private JLabel lblAvailableRooms;
    private JLabel lblFullRooms;
    private JLabel lblPartialRooms;

    // Block Cards Labels
    private JLabel lblBlockDInfo;
    private JLabel lblBlockGInfo;
    private JLabel lblBlockHInfo;

    // Allocation History Labels
    private JLabel lblActiveAlloc;
    private JLabel lblVacatedAlloc;
    private JLabel lblTotalAlloc;

    // Floor-Wise Table
    private DefaultTableModel modelFloor;
    private JTable            tableFloor;
    private JComboBox<String> cmbBlockFilter;
    private List<FloorStats>  allFloorStatsCache = new ArrayList<>();

    // Custom Visual Chart Panels
    private BlockOccupancyChartPanel chartBlockOccupancy;
    private AllocationDonutChartPanel chartAllocationDonut;

    public ReportsPanel() {
        this.reportService = new ReportService();
        setLayout(new BorderLayout());
        setBackground(BG_MAIN);

        initComponents();
        loadAllReportsData();
    }

    private void initComponents() {
        // Header Bar
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(CARD_BG);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
            new EmptyBorder(18, 24, 18, 24)
        ));

        JLabel lblTitle = new JLabel("System Reports & Analytics");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(TEXT_DARK);

        JLabel lblSub = new JLabel("Live database statistics, block summaries, floor breakdowns & allocation trends");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(TEXT_MUTED);

        JPanel titleContainer = new JPanel(new GridLayout(2, 1, 0, 3));
        titleContainer.setOpaque(false);
        titleContainer.add(lblTitle);
        titleContainer.add(lblSub);

        JButton btnRefresh = new JButton("🔄 Refresh Data");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setBackground(COLOR_BLUE);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.setBorder(new EmptyBorder(8, 16, 8, 16));
        btnRefresh.addActionListener(e -> loadAllReportsData());

        headerPanel.add(titleContainer, BorderLayout.WEST);
        headerPanel.add(btnRefresh, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Tabbed Pane for organized layout
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.setBackground(BG_MAIN);

        tabbedPane.addTab("📊 Executive Summary", buildExecutiveTab());
        tabbedPane.addTab("🏢 Floor-Wise Breakdown", buildFloorBreakdownTab());
        tabbedPane.addTab("📜 Allocation History", buildAllocationHistoryTab());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ── TAB 1: EXECUTIVE SUMMARY & BLOCK-WISE STATS ────────────────────────
    private JPanel buildExecutiveTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        // 1. Overall Stats Header
        JLabel lblOverallTitle = new JLabel("Overall Hostel Statistics");
        lblOverallTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblOverallTitle.setForeground(TEXT_DARK);
        lblOverallTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lblOverallTitle);
        panel.add(Box.createRigidArea(new Dimension(0, 12)));

        // Overall Cards Grid (2 rows x 4 cols)
        JPanel gridOverall = new JPanel(new GridLayout(2, 4, 14, 14));
        gridOverall.setOpaque(false);
        gridOverall.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        gridOverall.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblTotalStudents  = new JLabel("0", SwingConstants.CENTER);
        lblTotalRooms     = new JLabel("0", SwingConstants.CENTER);
        lblTotalCapacity  = new JLabel("0", SwingConstants.CENTER);
        lblOccupiedBeds   = new JLabel("0", SwingConstants.CENTER);
        lblAvailableBeds  = new JLabel("0", SwingConstants.CENTER);
        lblAvailableRooms = new JLabel("0", SwingConstants.CENTER);
        lblFullRooms      = new JLabel("0", SwingConstants.CENTER);
        lblPartialRooms   = new JLabel("0", SwingConstants.CENTER);

        gridOverall.add(createStatCard("Total Students", lblTotalStudents, COLOR_BLUE));
        gridOverall.add(createStatCard("Total Rooms", lblTotalRooms, COLOR_PURPLE));
        gridOverall.add(createStatCard("Total Capacity", lblTotalCapacity, COLOR_BLUE));
        gridOverall.add(createStatCard("Occupied Beds", lblOccupiedBeds, COLOR_AMBER));

        gridOverall.add(createStatCard("Available Beds", lblAvailableBeds, COLOR_GREEN));
        gridOverall.add(createStatCard("Available Rooms", lblAvailableRooms, COLOR_GREEN));
        gridOverall.add(createStatCard("Full Rooms", lblFullRooms, COLOR_RED));
        gridOverall.add(createStatCard("Partially Occupied", lblPartialRooms, COLOR_AMBER));

        panel.add(gridOverall);
        panel.add(Box.createRigidArea(new Dimension(0, 24)));

        // 2. Block-Wise Statistics Section
        JLabel lblBlockTitle = new JLabel("Block-Wise Breakdown (Blocks D, G, H)");
        lblBlockTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblBlockTitle.setForeground(TEXT_DARK);
        lblBlockTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lblBlockTitle);
        panel.add(Box.createRigidArea(new Dimension(0, 12)));

        JPanel gridBlocks = new JPanel(new GridLayout(1, 3, 16, 0));
        gridBlocks.setOpaque(false);
        gridBlocks.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        gridBlocks.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblBlockDInfo = new JLabel();
        lblBlockGInfo = new JLabel();
        lblBlockHInfo = new JLabel();

        gridBlocks.add(createBlockCard("Block D", "5 Floors", lblBlockDInfo, COLOR_BLUE));
        gridBlocks.add(createBlockCard("Block G", "4 Floors", lblBlockGInfo, COLOR_GREEN));
        gridBlocks.add(createBlockCard("Block H", "6 Floors", lblBlockHInfo, COLOR_PURPLE));

        panel.add(gridBlocks);
        panel.add(Box.createRigidArea(new Dimension(0, 24)));

        // 3. Visual Chart Component (Block Occupancy)
        JLabel lblChartTitle = new JLabel("Bed Occupancy Visual Chart");
        lblChartTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblChartTitle.setForeground(TEXT_DARK);
        lblChartTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lblChartTitle);
        panel.add(Box.createRigidArea(new Dimension(0, 12)));

        chartBlockOccupancy = new BlockOccupancyChartPanel();
        chartBlockOccupancy.setAlignmentX(Component.LEFT_ALIGNMENT);
        chartBlockOccupancy.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        panel.add(chartBlockOccupancy);

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        JPanel mainWrapper = new JPanel(new BorderLayout());
        mainWrapper.add(scrollPane, BorderLayout.CENTER);
        return mainWrapper;
    }

    // ── TAB 2: FLOOR-WISE BREAKDOWN ─────────────────────────────────────────
    private JPanel buildFloorBreakdownTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Filter Bar
        JPanel filterToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        filterToolbar.setOpaque(false);

        JLabel lblFilter = new JLabel("Filter by Block:");
        lblFilter.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblFilter.setForeground(TEXT_DARK);

        cmbBlockFilter = new JComboBox<>(new String[]{"All Blocks", "Block D", "Block G", "Block H"});
        cmbBlockFilter.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbBlockFilter.setPreferredSize(new Dimension(160, 32));
        cmbBlockFilter.addActionListener(e -> filterFloorStatsTable());

        filterToolbar.add(lblFilter);
        filterToolbar.add(cmbBlockFilter);
        panel.add(filterToolbar, BorderLayout.NORTH);

        // Table
        String[] columns = {"Block", "Floor Number", "Total Rooms", "Total Capacity", "Occupied Beds", "Available Beds", "Occupancy Rate"};
        modelFloor = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tableFloor = new JTable(modelFloor);
        tableFloor.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tableFloor.setRowHeight(32);
        tableFloor.setGridColor(BORDER_COLOR);

        JTableHeader header = tableFloor.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(new Color(241, 245, 249));
        header.setForeground(TEXT_DARK);
        header.setPreferredSize(new Dimension(0, 36));

        // Center align table columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < columns.length; i++) {
            tableFloor.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(tableFloor);
        scrollPane.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    // ── TAB 3: ALLOCATION HISTORY ───────────────────────────────────────────
    private JPanel buildAllocationHistoryTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel lblTitle = new JLabel("Allocation History Metrics");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(TEXT_DARK);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lblTitle);
        panel.add(Box.createRigidArea(new Dimension(0, 14)));

        // Metric Cards for Allocations
        JPanel gridAlloc = new JPanel(new GridLayout(1, 3, 16, 0));
        gridAlloc.setOpaque(false);
        gridAlloc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        gridAlloc.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblActiveAlloc  = new JLabel("0", SwingConstants.CENTER);
        lblVacatedAlloc = new JLabel("0", SwingConstants.CENTER);
        lblTotalAlloc   = new JLabel("0", SwingConstants.CENTER);

        gridAlloc.add(createStatCard("Active Allocations", lblActiveAlloc, COLOR_BLUE));
        gridAlloc.add(createStatCard("Vacated Allocations", lblVacatedAlloc, COLOR_AMBER));
        gridAlloc.add(createStatCard("Total Historical Allocations", lblTotalAlloc, COLOR_PURPLE));

        panel.add(gridAlloc);
        panel.add(Box.createRigidArea(new Dimension(0, 28)));

        // Custom Donut Chart for Allocations
        JLabel lblDonutTitle = new JLabel("Allocation Status Distribution Chart");
        lblDonutTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblDonutTitle.setForeground(TEXT_DARK);
        lblDonutTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lblDonutTitle);
        panel.add(Box.createRigidArea(new Dimension(0, 14)));

        chartAllocationDonut = new AllocationDonutChartPanel();
        chartAllocationDonut.setAlignmentX(Component.LEFT_ALIGNMENT);
        chartAllocationDonut.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        panel.add(chartAllocationDonut);

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }

    public void refreshData() {
        loadAllReportsData();
    }

    private void loadAllReportsData() {
        try {
            // 1. Overall & Block Stats
            DashboardStats overallStats = reportService.getOverallStats();
            updateOverallUI(overallStats);
            updateBlockCardsUI(overallStats);

            // Update Block Occupancy Chart
            if (chartBlockOccupancy != null) {
                chartBlockOccupancy.setBlockData(
                    overallStats.getBlockD(),
                    overallStats.getBlockG(),
                    overallStats.getBlockH()
                );
            }

            // 2. Floor-Wise Stats
            allFloorStatsCache = reportService.getFloorWiseStats();
            filterFloorStatsTable();

            // 3. Allocation History Stats
            AllocationHistoryStats allocStats = reportService.getAllocationHistoryStats();
            lblActiveAlloc.setText(String.valueOf(allocStats.getActiveAllocations()));
            lblVacatedAlloc.setText(String.valueOf(allocStats.getVacatedAllocations()));
            lblTotalAlloc.setText(String.valueOf(allocStats.getTotalAllocations()));

            if (chartAllocationDonut != null) {
                chartAllocationDonut.setAllocData(
                    allocStats.getActiveAllocations(),
                    allocStats.getVacatedAllocations()
                );
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Failed to load report statistics: " + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateOverallUI(DashboardStats stats) {
        lblTotalStudents.setText(String.valueOf(stats.getTotalStudents()));
        lblTotalRooms.setText(String.valueOf(stats.getTotalRooms()));
        lblTotalCapacity.setText(String.valueOf(stats.getTotalCapacity()));
        lblOccupiedBeds.setText(String.valueOf(stats.getOccupiedBeds()));
        lblAvailableBeds.setText(String.valueOf(stats.getAvailableBeds()));
        lblAvailableRooms.setText(String.valueOf(stats.getAvailableRooms()));
        lblFullRooms.setText(String.valueOf(stats.getFullRooms()));
        lblPartialRooms.setText(String.valueOf(stats.getPartiallyOccupiedRooms()));
    }

    private void updateBlockCardsUI(DashboardStats stats) {
        lblBlockDInfo.setText(formatBlockHtml(stats.getBlockD()));
        lblBlockGInfo.setText(formatBlockHtml(stats.getBlockG()));
        lblBlockHInfo.setText(formatBlockHtml(stats.getBlockH()));
    }

    private String formatBlockHtml(BlockStats b) {
        if (b == null) return "<html><i>No Data</i></html>";
        return String.format(
            "<html><div style='font-family:Segoe UI; font-size:11px; color:#334155; line-height:1.5;'>" +
            "<b>Total Rooms:</b> %d &nbsp;|&nbsp; <b>Capacity:</b> %d beds<br>" +
            "<b>Occupied:</b> %d beds &nbsp;|&nbsp; <font color='#059669'><b>Available:</b> %d beds</font><br>" +
            "<font color='#059669'>Available Rooms:</font> %d &nbsp;|&nbsp; <font color='#dc2626'>Full:</font> %d &nbsp;|&nbsp; <font color='#d97706'>Partial:</font> %d" +
            "</div></html>",
            b.getTotalRooms(), b.getTotalCapacity(),
            b.getOccupiedBeds(), b.getAvailableBeds(),
            b.getAvailableRooms(), b.getFullRooms(), b.getPartialRooms()
        );
    }

    private void filterFloorStatsTable() {
        modelFloor.setRowCount(0);
        String selectedFilter = (String) cmbBlockFilter.getSelectedItem();
        String targetBlock = null;

        if (selectedFilter != null && selectedFilter.startsWith("Block ")) {
            targetBlock = selectedFilter.replace("Block ", "").trim();
        }

        for (FloorStats f : allFloorStatsCache) {
            if (targetBlock != null && !f.getBlockName().equalsIgnoreCase(targetBlock)) {
                continue;
            }

            double occRate = f.getTotalCapacity() > 0 ?
                ((double) f.getOccupiedBeds() / f.getTotalCapacity()) * 100.0 : 0.0;

            modelFloor.addRow(new Object[]{
                "Block " + f.getBlockName(),
                "Floor " + f.getFloorNumber(),
                f.getTotalRooms(),
                f.getTotalCapacity(),
                f.getOccupiedBeds(),
                f.getAvailableBeds(),
                String.format("%.1f%%", occRate)
            });
        }
    }

    // ── HELPER UI BUILDERS ──────────────────────────────────────────────────
    private JPanel createStatCard(String title, JLabel lblValue, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(TEXT_MUTED);

        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblValue.setForeground(accentColor);

        // Top accent line
        JPanel topBar = new JPanel();
        topBar.setBackground(accentColor);
        topBar.setPreferredSize(new Dimension(0, 3));

        card.add(topBar, BorderLayout.NORTH);
        card.add(lblTitle, BorderLayout.CENTER);
        card.add(lblValue, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createBlockCard(String blockName, String subtitle, JLabel lblDetails, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(14, 16, 14, 16)
        ));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel lblName = new JLabel(blockName);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblName.setForeground(TEXT_DARK);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblSub.setForeground(accentColor);

        header.add(lblName, BorderLayout.WEST);
        header.add(lblSub, BorderLayout.EAST);

        card.add(header, BorderLayout.NORTH);
        card.add(lblDetails, BorderLayout.CENTER);
        return card;
    }

    // ── CUSTOM GRAPHICS 2D CHARTS ───────────────────────────────────────────

    /**
     * BlockOccupancyChartPanel — Custom antialiased Swing Bar Chart for Block D, G, H occupancy.
     */
    private static class BlockOccupancyChartPanel extends JPanel {

        private BlockStats statsD;
        private BlockStats statsG;
        private BlockStats statsH;

        public BlockOccupancyChartPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 200));
        }

        public void setBlockData(BlockStats d, BlockStats g, BlockStats h) {
            this.statsD = d;
            this.statsG = g;
            this.statsH = h;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            // Background Container
            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Float(0, 0, width - 1, height - 1, 12, 12));
            g2.setColor(BORDER_COLOR);
            g2.draw(new RoundRectangle2D.Float(0, 0, width - 1, height - 1, 12, 12));

            if (statsD == null || statsG == null || statsH == null) {
                g2.dispose();
                return;
            }

            BlockStats[] blocks = {statsD, statsG, statsH};
            Color[] colors = {COLOR_BLUE, COLOR_GREEN, COLOR_PURPLE};

            int startX = 60;
            int barWidth = Math.min(80, (width - 150) / 3);
            int gap = (width - 120 - (barWidth * 3)) / 2;
            int maxCap = Math.max(statsD.getTotalCapacity(), Math.max(statsG.getTotalCapacity(), statsH.getTotalCapacity()));
            if (maxCap == 0) maxCap = 100;

            int chartBottom = height - 45;
            int chartTop = 30;
            int maxBarHeight = chartBottom - chartTop;

            // Legend
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.setColor(COLOR_BLUE);
            g2.fillRect(width - 170, 12, 10, 10);
            g2.setColor(TEXT_DARK);
            g2.drawString("Occupied Beds", width - 155, 21);

            g2.setColor(BORDER_COLOR);
            g2.fillRect(width - 80, 12, 10, 10);
            g2.drawString("Available Beds", width - 65, 21);

            // Draw Bars
            for (int i = 0; i < 3; i++) {
                BlockStats b = blocks[i];
                int x = startX + i * (barWidth + gap);

                int totalH = (int) (((double) b.getTotalCapacity() / maxCap) * maxBarHeight);
                int occH   = (int) (((double) b.getOccupiedBeds() / maxCap) * maxBarHeight);

                // Available / Capacity background bar
                g2.setColor(new Color(241, 245, 249));
                g2.fill(new RoundRectangle2D.Float(x, chartBottom - totalH, barWidth, totalH, 8, 8));

                // Occupied bar
                g2.setColor(colors[i]);
                g2.fill(new RoundRectangle2D.Float(x, chartBottom - occH, barWidth, occH, 8, 8));

                // Labels
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.setColor(TEXT_DARK);
                g2.drawString("Block " + b.getBlockName(), x + (barWidth / 2) - 20, chartBottom + 20);

                // Percentage label inside/above bar
                double pct = b.getTotalCapacity() > 0 ? ((double) b.getOccupiedBeds() / b.getTotalCapacity()) * 100.0 : 0;
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                g2.setColor(colors[i]);
                g2.drawString(String.format("%.0f%%", pct), x + (barWidth / 2) - 12, chartBottom - occH - 6);
            }

            g2.dispose();
        }
    }

    /**
     * AllocationDonutChartPanel — Custom antialiased Swing Donut Chart for Active vs Vacated.
     */
    private static class AllocationDonutChartPanel extends JPanel {

        private int activeCount;
        private int vacatedCount;

        public AllocationDonutChartPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 240));
        }

        public void setAllocData(int active, int vacated) {
            this.activeCount  = active;
            this.vacatedCount = vacated;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            // Background Card
            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Float(0, 0, width - 1, height - 1, 12, 12));
            g2.setColor(BORDER_COLOR);
            g2.draw(new RoundRectangle2D.Float(0, 0, width - 1, height - 1, 12, 12));

            int total = activeCount + vacatedCount;
            if (total == 0) {
                g2.setFont(new Font("Segoe UI", Font.ITALIC, 13));
                g2.setColor(TEXT_MUTED);
                g2.drawString("No allocation history data available", width / 2 - 100, height / 2);
                g2.dispose();
                return;
            }

            int diameter = Math.min(width, height) - 60;
            int cx = 70;
            int cy = (height - diameter) / 2;

            double activeAngle  = ((double) activeCount / total) * 360.0;
            double vacatedAngle = 360.0 - activeAngle;

            // Draw Active Arc
            g2.setColor(COLOR_BLUE);
            g2.fill(new Arc2D.Double(cx, cy, diameter, diameter, 90, -activeAngle, Arc2D.PIE));

            // Draw Vacated Arc
            g2.setColor(COLOR_AMBER);
            g2.fill(new Arc2D.Double(cx, cy, diameter, diameter, 90 - activeAngle, -vacatedAngle, Arc2D.PIE));

            // Inner Donut Hole
            int innerD = (int) (diameter * 0.6);
            int innerX = cx + (diameter - innerD) / 2;
            int innerY = cy + (diameter - innerD) / 2;
            g2.setColor(Color.WHITE);
            g2.fillOval(innerX, innerY, innerD, innerD);

            // Center Text
            g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
            g2.setColor(TEXT_DARK);
            g2.drawString(String.valueOf(total), innerX + (innerD / 2) - 10, innerY + (innerD / 2) + 2);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.setColor(TEXT_MUTED);
            g2.drawString("TOTAL", innerX + (innerD / 2) - 16, innerY + (innerD / 2) + 16);

            // Legends on the right
            int legendX = cx + diameter + 60;
            int legendY = height / 2 - 20;

            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));

            // Active Legend
            g2.setColor(COLOR_BLUE);
            g2.fillOval(legendX, legendY, 12, 12);
            g2.setColor(TEXT_DARK);
            double activePct = ((double) activeCount / total) * 100.0;
            g2.drawString(String.format("Active: %d (%.1f%%)", activeCount, activePct), legendX + 20, legendY + 11);

            // Vacated Legend
            g2.setColor(COLOR_AMBER);
            g2.fillOval(legendX, legendY + 30, 12, 12);
            g2.setColor(TEXT_DARK);
            double vacatedPct = ((double) vacatedCount / total) * 100.0;
            g2.drawString(String.format("Vacated: %d (%.1f%%)", vacatedCount, vacatedPct), legendX + 20, legendY + 41);

            g2.dispose();
        }
    }
}
