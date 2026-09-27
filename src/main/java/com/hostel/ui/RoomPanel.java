package com.hostel.ui;

import com.hostel.model.RoomDetail;
import com.hostel.model.RoomOccupantDetail;
import com.hostel.service.RoomInitializationService;
import com.hostel.service.RoomService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * RoomPanel — Complete Room Management UI.
 *
 * Features:
 *   - Dynamic cascading Block → Floor dropdown selection
 *   - Multi-criteria filtering (Block, Floor, Status, Room Search)
 *   - Live status indicator badges (AVAILABLE, PARTIALLY_OCCUPIED, FULL, MAINTENANCE)
 *   - Update maintenance status (with occupied > 0 safety guard)
 *   - View room occupancy details dialog (double-click row or click button)
 *   - Safe infrastructure room verification & initialization button
 */
public class RoomPanel extends JPanel {

    // ── Theme Colors ───────────────────────────────────────────────────────
    private static final Color BG_CANVAS     = UIUtil.BG_DARK;
    private static final Color CARD_BG       = UIUtil.CARD_BG;
    private static final Color CARD_BORDER   = UIUtil.CARD_BORDER;
    private static final Color TEXT_DARK     = UIUtil.TEXT_MAIN;
    private static final Color TEXT_MUTED    = UIUtil.TEXT_MUTED;
    private static final Color ACCENT_BLUE   = UIUtil.ACCENT_PRIMARY;
    private static final Color BTN_AMBER     = UIUtil.WARNING_AMBER;
    private static final Color BTN_INDIGO    = UIUtil.ACCENT_PRIMARY;
    private static final Color BTN_EMERALD   = UIUtil.SUCCESS_GREEN;
    private static final Color TABLE_HDR_BG  = UIUtil.BG_DARK;

    // ── Filter Controls ────────────────────────────────────────────────────
    private JTextField txtSearch;
    private JComboBox<String> cmbBlock;
    private JComboBox<String> cmbFloor;
    private JComboBox<String> cmbStatus;
    private JButton btnClearFilters;

    // ── Stats Labels ───────────────────────────────────────────────────────
    private JLabel lblTotalCount;
    private JLabel lblAvailCount;
    private JLabel lblPartialCount;
    private JLabel lblFullCount;
    private JLabel lblMaintCount;

    // ── Table ──────────────────────────────────────────────────────────────
    private JTable tableRooms;
    private DefaultTableModel modelRooms;

    // ── Action Buttons ─────────────────────────────────────────────────────
    private JButton btnVerifyInitRooms;
    private JButton btnToggleMaintenance;
    private JButton btnViewOccupancy;

    private final RoomService roomService = new RoomService();
    private final RoomInitializationService initService = new RoomInitializationService();
    private boolean isUpdatingFloors = false;

    public RoomPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(BG_CANVAS);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        add(buildHeaderPanel(), BorderLayout.NORTH);
        add(buildCenterContent(), BorderLayout.CENTER);

        // Initial Load
        loadRoomsData();
    }

    // ── Header Panel ───────────────────────────────────────────────────────

    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        // Left Title
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel title = new JLabel("Room Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_DARK);

        JLabel sub = new JLabel("Filter, search, and manage room maintenance status across all blocks and floors");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(TEXT_MUTED);

        left.add(title);
        left.add(Box.createVerticalStrut(2));
        left.add(sub);

        // Right Stats Badges
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);

        lblTotalCount   = makeBadge("Total Rooms", "452");
        lblAvailCount   = makeBadge("Available", "0");
        lblPartialCount = makeBadge("Partial", "0");
        lblFullCount    = makeBadge("Full", "0");
        lblMaintCount   = makeBadge("Maintenance", "0");

        right.add(lblTotalCount);
        right.add(lblAvailCount);
        right.add(lblPartialCount);
        right.add(lblFullCount);
        right.add(lblMaintCount);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    // ── Center Content ─────────────────────────────────────────────────────

    private JPanel buildCenterContent() {
        JPanel container = new JPanel(new BorderLayout(0, 14));
        container.setOpaque(false);

        container.add(buildFilterToolbar(), BorderLayout.NORTH);
        container.add(buildTableCard(), BorderLayout.CENTER);
        container.add(buildActionToolbar(), BorderLayout.SOUTH);

        return container;
    }

    // ── Filter Toolbar ─────────────────────────────────────────────────────

    private JPanel buildFilterToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        toolbar.setBackground(CARD_BG);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 1),
            new EmptyBorder(4, 12, 4, 12)
        ));

        JLabel lblSearch = new JLabel("🔍 Search Room:");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSearch.setForeground(TEXT_DARK);

        txtSearch = new JTextField(10);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearch.setPreferredSize(new Dimension(130, 32));
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 1),
            new EmptyBorder(4, 8, 4, 8)
        ));
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { loadRoomsData(); }
            public void removeUpdate(DocumentEvent e) { loadRoomsData(); }
            public void changedUpdate(DocumentEvent e){ loadRoomsData(); }
        });

        JLabel lblBlock = new JLabel("🏢 Block:");
        lblBlock.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblBlock.setForeground(TEXT_DARK);

        cmbBlock = new JComboBox<>(new String[]{"All Blocks", "Block D", "Block G", "Block H"});
        cmbBlock.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbBlock.setPreferredSize(new Dimension(120, 32));
        cmbBlock.addActionListener(e -> onBlockSelected());

        JLabel lblFloor = new JLabel("📶 Floor:");
        lblFloor.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblFloor.setForeground(TEXT_DARK);

        cmbFloor = new JComboBox<>(new String[]{"All Floors"});
        cmbFloor.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbFloor.setPreferredSize(new Dimension(120, 32));
        cmbFloor.addActionListener(e -> {
            if (!isUpdatingFloors) loadRoomsData();
        });

        JLabel lblStatus = new JLabel("📌 Status:");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatus.setForeground(TEXT_DARK);

        cmbStatus = new JComboBox<>(new String[]{"All Statuses", "AVAILABLE", "PARTIALLY_OCCUPIED", "FULL", "MAINTENANCE"});
        cmbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbStatus.setPreferredSize(new Dimension(160, 32));
        cmbStatus.addActionListener(e -> loadRoomsData());

        btnClearFilters = new JButton("🔄 Reset Filters");
        btnClearFilters.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClearFilters.setFocusPainted(false);
        btnClearFilters.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClearFilters.setPreferredSize(new Dimension(120, 32));
        btnClearFilters.addActionListener(e -> resetFilters());

        toolbar.add(lblSearch);
        toolbar.add(txtSearch);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(lblBlock);
        toolbar.add(cmbBlock);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(lblFloor);
        toolbar.add(cmbFloor);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(lblStatus);
        toolbar.add(cmbStatus);
        toolbar.add(Box.createHorizontalStrut(12));
        toolbar.add(btnClearFilters);

        return toolbar;
    }

    // ── Table Panel ────────────────────────────────────────────────────────

    private JPanel buildTableCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 1),
            new EmptyBorder(8, 8, 8, 8)
        ));

        String[] cols = {
            "Room ID", "Block", "Floor", "Room Number",
            "Capacity", "Occupied", "Available Beds", "Status"
        };

        modelRooms = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        tableRooms = new JTable(modelRooms);
        tableRooms.setRowHeight(34);
        tableRooms.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tableRooms.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableRooms.setGridColor(new Color(0xf1f5f9));
        tableRooms.setSelectionBackground(new Color(0xdbeafe));
        tableRooms.setSelectionForeground(new Color(0x1e3a8a));

        // Hide Room ID
        tableRooms.getColumnModel().getColumn(0).setMinWidth(0);
        tableRooms.getColumnModel().getColumn(0).setMaxWidth(0);
        tableRooms.getColumnModel().getColumn(0).setWidth(0);

        JTableHeader header = tableRooms.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(TABLE_HDR_BG);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 36));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tableRooms.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        tableRooms.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        tableRooms.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        tableRooms.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        tableRooms.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        tableRooms.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);

        tableRooms.getColumnModel().getColumn(7).setCellRenderer(new StatusCellRenderer());

        tableRooms.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSelection = tableRooms.getSelectedRow() != -1;
            btnToggleMaintenance.setEnabled(hasSelection);
            btnViewOccupancy.setEnabled(hasSelection);
        });

        tableRooms.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tableRooms.getSelectedRow() != -1) {
                    showOccupancyDialog();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tableRooms);
        scroll.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    // ── Action Toolbar (Bottom) ────────────────────────────────────────────

    private JPanel buildActionToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        bar.setOpaque(false);

        btnVerifyInitRooms   = makeCustomButton("⚡ Sync Infrastructure Rooms", BTN_EMERALD, 220);
        btnToggleMaintenance = makeCustomButton("🔧 Toggle Maintenance Status", BTN_AMBER, 220);
        btnViewOccupancy     = makeCustomButton("👥 View Room Occupants",     BTN_INDIGO, 200);

        btnToggleMaintenance.setEnabled(false);
        btnViewOccupancy.setEnabled(false);

        btnVerifyInitRooms.addActionListener(e -> performVerifyInitRooms());
        btnToggleMaintenance.addActionListener(e -> performToggleMaintenance());
        btnViewOccupancy.addActionListener(e -> showOccupancyDialog());

        bar.add(btnVerifyInitRooms);
        bar.add(btnToggleMaintenance);
        bar.add(btnViewOccupancy);

        return bar;
    }

    // ── Infrastructure Sync Handler ────────────────────────────────────────

    private void performVerifyInitRooms() {
        btnVerifyInitRooms.setEnabled(false);
        btnVerifyInitRooms.setText("⏳ Verifying Rooms...");

        SwingWorker<RoomInitializationService.RoomInitializationResult, Void> worker =
            new SwingWorker<>() {
                @Override
                protected RoomInitializationService.RoomInitializationResult doInBackground() throws Exception {
                    return initService.initializeOrVerifyRooms();
                }

                @Override
                protected void done() {
                    btnVerifyInitRooms.setEnabled(true);
                    btnVerifyInitRooms.setText("⚡ Sync Infrastructure Rooms");

                    try {
                        RoomInitializationService.RoomInitializationResult res = get();
                        String msg = String.format(
                            "Hostel Infrastructure Verification Complete:\n\n" +
                            "  🏢 Block D : %d rooms\n" +
                            "  🏢 Block G : %d rooms\n" +
                            "  🏢 Block H : %d rooms\n" +
                            "  -----------------------------\n" +
                            "  📊 Total   : %d rooms\n\n" +
                            "  ✨ Newly Generated : %d\n" +
                            "  🛡️ Skipped (Existing) : %d",
                            res.getBlockDRooms(), res.getBlockGRooms(), res.getBlockHRooms(),
                            res.getTotalRooms(), res.getRoomsGenerated(), res.getRoomsSkipped()
                        );
                        JOptionPane.showMessageDialog(RoomPanel.this, msg, "Infrastructure Verification", JOptionPane.INFORMATION_MESSAGE);
                        loadRoomsData();
                    } catch (Exception ex) {
                        showError("Initialization Error: " + ex.getMessage());
                    }
                }
            };
        worker.execute();
    }

    // ── Dynamic Cascading Dropdown Handler ─────────────────────────────────

    private void onBlockSelected() {
        isUpdatingFloors = true;
        cmbFloor.removeAllItems();
        cmbFloor.addItem("All Floors");

        String selectedBlock = cmbBlock.getSelectedItem().toString();

        if (selectedBlock.contains("D")) {
            for (int i = 1; i <= 5; i++) cmbFloor.addItem("Floor " + i);
        } else if (selectedBlock.contains("G")) {
            for (int i = 1; i <= 4; i++) cmbFloor.addItem("Floor " + i);
        } else if (selectedBlock.contains("H")) {
            for (int i = 1; i <= 6; i++) cmbFloor.addItem("Floor " + i);
        }

        isUpdatingFloors = false;
        loadRoomsData();
    }

    // ── Data Loading & Filtering ───────────────────────────────────────────

    private void loadRoomsData() {
        modelRooms.setRowCount(0);

        String search = txtSearch.getText().trim();

        String blockSel = cmbBlock.getSelectedItem() != null ? cmbBlock.getSelectedItem().toString() : "All Blocks";
        String blockName = blockSel.startsWith("Block ") ? blockSel.replace("Block ", "") : null;

        String floorSel = cmbFloor.getSelectedItem() != null ? cmbFloor.getSelectedItem().toString() : "All Floors";
        Integer floorNum = floorSel.startsWith("Floor ") ? Integer.parseInt(floorSel.replace("Floor ", "")) : null;

        String statusSel = cmbStatus.getSelectedItem() != null ? cmbStatus.getSelectedItem().toString() : "All Statuses";
        String statusFilter = "All Statuses".equalsIgnoreCase(statusSel) ? null : statusSel;

        try {
            List<RoomDetail> rooms = roomService.searchAndFilterRooms(blockName, floorNum, statusFilter, search);

            int totalCount   = rooms.size();
            int availCount   = 0;
            int partialCount = 0;
            int fullCount    = 0;
            int maintCount   = 0;

            for (RoomDetail r : rooms) {
                switch (r.getStatus()) {
                    case "AVAILABLE"          -> availCount++;
                    case "PARTIALLY_OCCUPIED" -> partialCount++;
                    case "FULL"               -> fullCount++;
                    case "MAINTENANCE"        -> maintCount++;
                }

                modelRooms.addRow(new Object[]{
                    r.getId(),
                    "Block " + r.getBlockName(),
                    "Floor " + r.getFloorNumber(),
                    r.getRoomNumber(),
                    r.getCapacity() + " Beds",
                    r.getOccupied(),
                    r.getAvailableBeds(),
                    r.getStatus()
                });
            }

            setBadgeVal(lblTotalCount,   String.valueOf(totalCount));
            setBadgeVal(lblAvailCount,   String.valueOf(availCount));
            setBadgeVal(lblPartialCount, String.valueOf(partialCount));
            setBadgeVal(lblFullCount,    String.valueOf(fullCount));
            setBadgeVal(lblMaintCount,   String.valueOf(maintCount));

        } catch (Exception ex) {
            showError("Failed to fetch rooms: " + ex.getMessage());
        }
    }

    private void resetFilters() {
        txtSearch.setText("");
        cmbBlock.setSelectedIndex(0);
        cmbStatus.setSelectedIndex(0);
        onBlockSelected();
    }

    // ── Maintenance Toggle Action ──────────────────────────────────────────

    private void performToggleMaintenance() {
        int row = tableRooms.getSelectedRow();
        if (row == -1) return;

        int roomId = (int) modelRooms.getValueAt(row, 0);
        String roomNum = (String) modelRooms.getValueAt(row, 3);
        String currentStatus = (String) modelRooms.getValueAt(row, 7);

        boolean isMaint = "MAINTENANCE".equalsIgnoreCase(currentStatus);
        String actionMsg = isMaint
            ? "Restore Room " + roomNum + " to AVAILABLE status?"
            : "Mark Room " + roomNum + " as MAINTENANCE?\n(No new allocations will be allowed)";

        int confirm = JOptionPane.showConfirmDialog(
            this, actionMsg, "Confirm Status Change", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                roomService.toggleMaintenanceStatus(roomId);
                showSuccess("Status updated for Room " + roomNum + " successfully!");
                loadRoomsData();
            } catch (IllegalArgumentException ex) {
                showWarning(ex.getMessage());
            } catch (Exception ex) {
                showError("Database Error: " + ex.getMessage());
            }
        }
    }

    // ── View Occupants Dialog ──────────────────────────────────────────────

    private void showOccupancyDialog() {
        int row = tableRooms.getSelectedRow();
        if (row == -1) return;

        int roomId = (int) modelRooms.getValueAt(row, 0);
        String roomNum  = (String) modelRooms.getValueAt(row, 3);
        String capacity = (String) modelRooms.getValueAt(row, 4);
        int occupied    = (int) modelRooms.getValueAt(row, 5);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Room " + roomNum + " Occupancy Details", true);
        dialog.setLayout(new BorderLayout(0, 16));
        dialog.setSize(650, 400);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 12));
        mainPanel.setBackground(BG_CANVAS);
        mainPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel headerCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
        headerCard.setBackground(CARD_BG);
        headerCard.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));

        JLabel info = new JLabel("<html><b>Room Number:</b> " + roomNum + " &nbsp; | &nbsp; <b>Capacity:</b> " +
                                 capacity + " &nbsp; | &nbsp; <b>Active Occupants:</b> " + occupied + "</html>");
        info.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        info.setForeground(TEXT_DARK);
        headerCard.add(info);

        String[] cols = {"Student ID", "Student Name", "Course", "Year", "Phone", "Allocated On"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(TABLE_HDR_BG);
        table.getTableHeader().setForeground(Color.WHITE);

        try {
            List<RoomOccupantDetail> list = roomService.getRoomOccupants(roomId);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            for (RoomOccupantDetail occ : list) {
                model.addRow(new Object[]{
                    occ.getStudentId(),
                    occ.getName(),
                    occ.getCourse(),
                    "Year " + occ.getYear(),
                    occ.getPhone(),
                    occ.getAllocDate() != null ? sdf.format(occ.getAllocDate()) : "-"
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(dialog, "Failed to load occupants: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));

        JButton btnClose = new JButton("Close");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClose.addActionListener(e -> dialog.dispose());

        JPanel btm = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btm.setOpaque(false);
        btm.add(btnClose);

        mainPanel.add(headerCard, BorderLayout.NORTH);
        mainPanel.add(scroll, BorderLayout.CENTER);
        mainPanel.add(btm, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    // ── Table Status Cell Renderer ─────────────────────────────────────────

    private static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setHorizontalAlignment(JLabel.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));

            String status = value != null ? value.toString() : "";

            if (!isSelected) {
                switch (status) {
                    case "AVAILABLE" -> {
                        lbl.setForeground(new Color(0x15803d));
                        lbl.setBackground(new Color(0xdcfce7));
                    }
                    case "PARTIALLY_OCCUPIED" -> {
                        lbl.setForeground(new Color(0xb45309));
                        lbl.setBackground(new Color(0xfef3c7));
                    }
                    case "FULL" -> {
                        lbl.setForeground(new Color(0xb91c1c));
                        lbl.setBackground(new Color(0xfee2e2));
                    }
                    case "MAINTENANCE" -> {
                        lbl.setForeground(new Color(0x475569));
                        lbl.setBackground(new Color(0xe2e8f0));
                    }
                    default -> {
                        lbl.setForeground(TEXT_DARK);
                        lbl.setBackground(Color.WHITE);
                    }
                }
                lbl.setOpaque(true);
            }
            return lbl;
        }
    }

    // ── UI Helpers ─────────────────────────────────────────────────────────

    private JLabel makeBadge(String title, String val) {
        JLabel lbl = new JLabel("<html><center><font color='#64748b' size='2'>" + title + "</font><br><font color='#0f172a' size='4'><b>" + val + "</b></font></center></html>");
        lbl.setPreferredSize(new Dimension(85, 42));
        return lbl;
    }

    private void setBadgeVal(JLabel lbl, String val) {
        String title = lbl.getText().replaceAll(".*<font color='#64748b' size='2'>(.*?)</font>.*", "$1");
        lbl.setText("<html><center><font color='#64748b' size='2'>" + title + "</font><br><font color='#0f172a' size='4'><b>" + val + "</b></font></center></html>");
    }

    private JButton makeCustomButton(String text, Color bg, int width) {
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
                Color paintBg = !isEnabled() ? new Color(0xcbd5e1)
                              : hover        ? bg.darker()
                              :                bg;
                g2.setColor(paintBg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 6, 6));
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
        btn.setPreferredSize(new Dimension(width, 36));
        btn.setOpaque(false);
        return btn;
    }

    private void showSuccess(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showWarning(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
