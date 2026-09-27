package com.hostel.ui;

import com.hostel.dao.FloorDAO.FloorStat;
import com.hostel.model.Block;
import com.hostel.service.BlockService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BlockPanel — Block and Floor Management UI.
 *
 * Displays live hostel infrastructure from MySQL:
 *   - Block D (5 Floors, 60 Rooms, 144 Beds)
 *   - Block G (4 Floors, 212 Rooms, 477 Beds)
 *   - Block H (6 Floors, 180 Rooms, 450 Beds)
 *
 * Features:
 *   - Tabbed block selector (D, G, H)
 *   - Detailed floor-by-floor breakdown table
 *   - Edit floor capacity and rooms count with live DB updates
 *   - Safe deletion handling (prevents deleting blocks/floors with dependent rooms or active allocations)
 */
public class BlockPanel extends JPanel {

    // ── Colors ─────────────────────────────────────────────────────────────
    private static final Color BG_CANVAS     = new Color(0xf8fafc);
    private static final Color CARD_BG       = Color.WHITE;
    private static final Color CARD_BORDER   = new Color(0xe2e8f0);
    private static final Color TEXT_DARK     = new Color(0x0f172a);
    private static final Color TEXT_MUTED    = new Color(0x64748b);
    private static final Color ACCENT_BLUE   = new Color(0x3b82f6);
    private static final Color BTN_AMBER     = new Color(0xf59e0b);
    private static final Color BTN_RED       = new Color(0xef4444);
    private static final Color TABLE_HDR_BG  = new Color(0x1e293b);

    // ── Components ─────────────────────────────────────────────────────────
    private final Map<String, JButton> blockTabBtns = new HashMap<>();
    private String selectedBlockName = "D";

    private JLabel lblBlockTitle;
    private JLabel lblBlockSubtitle;
    private JLabel lblTotalFloors;
    private JLabel lblTotalRooms;
    private JLabel lblTotalBeds;
    private JLabel lblOccupiedBeds;
    private JLabel lblAvailableBeds;
    private JProgressBar progressOccupancy;

    private JTable tableFloors;
    private DefaultTableModel modelFloors;

    // Edit Floor Controls
    private JTextField txtFloorNum;
    private JTextField txtRoomsCount;
    private JTextField txtRoomCapacity;
    private JButton    btnUpdateFloor;
    private JButton    btnDeleteFloor;

    private int selectedFloorId = -1;

    private final BlockService blockService = new BlockService();

    public BlockPanel() {
        setLayout(new BorderLayout());
        setBackground(BG_CANVAS);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        add(buildHeaderPanel(), BorderLayout.NORTH);
        add(buildMainContent(), BorderLayout.CENTER);

        // Initial Load for Block D
        selectBlock("D");
    }

    // ── Header Panel ───────────────────────────────────────────────────────

    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 16, 0));

        // Left Title
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel title = new JLabel("Block & Floor Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_DARK);

        JLabel sub = new JLabel("Database-backed overview of hostel blocks (D, G, H) and floor capacity configurations");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(TEXT_MUTED);

        left.add(title);
        left.add(Box.createVerticalStrut(2));
        left.add(sub);

        // Right Block Selector Tabs (D, G, H)
        JPanel rightTabs = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightTabs.setOpaque(false);

        String[] blocks = {"D", "G", "H"};
        for (String b : blocks) {
            JButton btn = makeTabButton("🏢 Block " + b, b);
            blockTabBtns.put(b, btn);
            rightTabs.add(btn);
        }

        header.add(left, BorderLayout.WEST);
        header.add(rightTabs, BorderLayout.EAST);
        return header;
    }

    // ── Main Content Panel ─────────────────────────────────────────────────

    private JPanel buildMainContent() {
        JPanel container = new JPanel(new BorderLayout(0, 16));
        container.setOpaque(false);

        container.add(buildSummaryCard(), BorderLayout.NORTH);
        container.add(buildFloorGridAndEditPanel(), BorderLayout.CENTER);

        return container;
    }

    // ── Summary Header Card ────────────────────────────────────────────────

    private JPanel buildSummaryCard() {
        JPanel card = new JPanel(new BorderLayout(20, 0));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 1),
            new EmptyBorder(16, 20, 16, 20)
        ));

        // Left Block Meta Info
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        lblBlockTitle = new JLabel("Block D Overview");
        lblBlockTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblBlockTitle.setForeground(TEXT_DARK);

        lblBlockSubtitle = new JLabel("5 Floors  •  12 Rooms/Floor  •  3-Seater (F1-2) & 2-Seater (F3-5)");
        lblBlockSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblBlockSubtitle.setForeground(TEXT_MUTED);

        left.add(lblBlockTitle);
        left.add(Box.createVerticalStrut(4));
        left.add(lblBlockSubtitle);

        // Stats Badges (Floors, Rooms, Beds, Occupied, Avail)
        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        statsRow.setOpaque(false);

        lblTotalFloors   = makeStatBadge("Total Floors", "5");
        lblTotalRooms    = makeStatBadge("Total Rooms", "60");
        lblTotalBeds     = makeStatBadge("Total Beds", "144");
        lblOccupiedBeds  = makeStatBadge("Occupied", "0");
        lblAvailableBeds = makeStatBadge("Available", "144");

        statsRow.add(lblTotalFloors);
        statsRow.add(lblTotalRooms);
        statsRow.add(lblTotalBeds);
        statsRow.add(lblOccupiedBeds);
        statsRow.add(lblAvailableBeds);

        // Progress bar container
        JPanel progressPanel = new JPanel(new BorderLayout(8, 0));
        progressPanel.setOpaque(false);
        progressPanel.setBorder(new EmptyBorder(8, 0, 0, 0));

        JLabel progressLbl = new JLabel("Occupancy Rate:");
        progressLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        progressLbl.setForeground(TEXT_MUTED);

        progressOccupancy = new JProgressBar(0, 100);
        progressOccupancy.setStringPainted(true);
        progressOccupancy.setFont(new Font("Segoe UI", Font.BOLD, 11));
        progressOccupancy.setForeground(ACCENT_BLUE);
        progressOccupancy.setPreferredSize(new Dimension(200, 18));

        progressPanel.add(progressLbl, BorderLayout.WEST);
        progressPanel.add(progressOccupancy, BorderLayout.CENTER);

        left.add(Box.createVerticalStrut(8));
        left.add(progressPanel);

        card.add(left, BorderLayout.WEST);
        card.add(statsRow, BorderLayout.EAST);

        return card;
    }

    // ── Floor Table & Edit Split ───────────────────────────────────────────

    private JPanel buildFloorGridAndEditPanel() {
        JPanel split = new JPanel(new BorderLayout(16, 0));
        split.setOpaque(false);

        // Floor Table (Center/Left)
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(CARD_BG);
        tableCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 1),
            new EmptyBorder(12, 12, 12, 12)
        ));

        String[] cols = {"Floor ID", "Floor #", "Rooms Count", "Default Capacity", "Total Beds", "Occupied Beds", "Available Beds", "Occupancy %"};
        modelFloors = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        tableFloors = new JTable(modelFloors);
        tableFloors.setRowHeight(34);
        tableFloors.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tableFloors.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableFloors.setGridColor(new Color(0xf1f5f9));
        tableFloors.setSelectionBackground(new Color(0xdbeafe));
        tableFloors.setSelectionForeground(new Color(0x1e3a8a));

        // Hide Floor ID column
        tableFloors.getColumnModel().getColumn(0).setMinWidth(0);
        tableFloors.getColumnModel().getColumn(0).setMaxWidth(0);
        tableFloors.getColumnModel().getColumn(0).setWidth(0);

        JTableHeader header = tableFloors.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(TABLE_HDR_BG);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 36));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 1; i < cols.length; i++) {
            tableFloors.getColumnModel().getColumn(i).setCellRenderer(center);
        }

        JScrollPane scroll = new JScrollPane(tableFloors);
        scroll.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));
        tableCard.add(scroll, BorderLayout.CENTER);

        // Table Selection Listener
        tableFloors.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tableFloors.getSelectedRow() != -1) {
                populateEditForm(tableFloors.getSelectedRow());
            }
        });

        // Edit Floor Panel (Right)
        split.add(tableCard, BorderLayout.CENTER);
        split.add(buildEditFloorPanel(), BorderLayout.EAST);

        return split;
    }

    // ── Floor Edit Panel (Right Column) ───────────────────────────────────

    private JPanel buildEditFloorPanel() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 1),
            new EmptyBorder(18, 18, 18, 18)
        ));
        card.setPreferredSize(new Dimension(300, 0));

        JLabel title = new JLabel("⚙️ Edit Floor Properties");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(TEXT_DARK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel desc = new JLabel("Select a floor from the table to modify its default room capacity or room count.");
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        desc.setForeground(TEXT_MUTED);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtFloorNum     = makeReadOnlyField();
        txtRoomsCount   = makeTextField();
        txtRoomCapacity = makeTextField();

        addFormField(card, "Selected Floor #", txtFloorNum);
        addFormField(card, "Rooms Count *",    txtRoomsCount);
        addFormField(card, "Room Capacity (Seats/Room) *", txtRoomCapacity);

        card.add(Box.createVerticalStrut(14));

        btnUpdateFloor = makeCustomButton("💾 Save Floor Changes", BTN_AMBER, 260);
        btnDeleteFloor = makeCustomButton("🗑️ Safely Delete Floor", BTN_RED, 260);

        btnUpdateFloor.setEnabled(false);
        btnDeleteFloor.setEnabled(false);

        btnUpdateFloor.addActionListener(e -> performUpdateFloor());
        btnDeleteFloor.addActionListener(e -> performDeleteFloor());

        card.add(btnUpdateFloor);
        card.add(Box.createVerticalStrut(8));
        card.add(btnDeleteFloor);

        return card;
    }

    // ── Actions ────────────────────────────────────────────────────────────

    private void selectBlock(String blockName) {
        this.selectedBlockName = blockName;

        // Update tab styles
        for (Map.Entry<String, JButton> entry : blockTabBtns.entrySet()) {
            boolean active = entry.getKey().equals(blockName);
            entry.getValue().setBackground(active ? ACCENT_BLUE : new Color(0xe2e8f0));
            entry.getValue().setForeground(active ? Color.WHITE : TEXT_DARK);
        }

        loadBlockData(blockName);
        clearEditForm();
    }

    private void loadBlockData(String blockName) {
        modelFloors.setRowCount(0);

        try {
            Block block = blockService.getBlockByName(blockName);
            if (block == null) return;

            List<FloorStat> stats = blockService.getFloorStatsByBlock(block.getId());

            int totalFloors = stats.size();
            int totalRooms  = 0;
            int totalBeds   = 0;
            int totalOcc    = 0;

            for (FloorStat s : stats) {
                totalRooms += s.getRoomsCount();
                totalBeds  += s.getTotalBeds();
                totalOcc   += s.getOccupiedBeds();

                double pct = s.getTotalBeds() > 0 ? ((double) s.getOccupiedBeds() / s.getTotalBeds()) * 100.0 : 0.0;

                modelFloors.addRow(new Object[]{
                    s.getFloorId(),
                    "Floor " + s.getFloorNumber(),
                    s.getRoomsCount(),
                    s.getRoomCapacity() + " Seats",
                    s.getTotalBeds(),
                    s.getOccupiedBeds(),
                    s.getAvailableBeds(),
                    String.format("%.1f%%", pct)
                });
            }

            int totalAvail = Math.max(0, totalBeds - totalOcc);

            // Update Summary Header
            lblBlockTitle.setText("Block " + blockName + " Infrastructure Overview");
            lblBlockSubtitle.setText("Total " + totalFloors + " Floors  •  " + totalRooms + " Rooms  •  " + totalBeds + " Beds Capacity");

            setStatBadgeValue(lblTotalFloors,   String.valueOf(totalFloors));
            setStatBadgeValue(lblTotalRooms,    String.valueOf(totalRooms));
            setStatBadgeValue(lblTotalBeds,     String.valueOf(totalBeds));
            setStatBadgeValue(lblOccupiedBeds,  String.valueOf(totalOcc));
            setStatBadgeValue(lblAvailableBeds, String.valueOf(totalAvail));

            int pctOverall = totalBeds > 0 ? (int) Math.round(((double) totalOcc / totalBeds) * 100.0) : 0;
            progressOccupancy.setValue(pctOverall);
            progressOccupancy.setString(pctOverall + "% Occupied (" + totalOcc + "/" + totalBeds + " Beds)");

        } catch (Exception e) {
            showError("Failed to load block statistics:\n" + e.getMessage());
        }
    }

    private void populateEditForm(int row) {
        selectedFloorId = (int) modelFloors.getValueAt(row, 0);
        txtFloorNum.setText(modelFloors.getValueAt(row, 1).toString());
        txtRoomsCount.setText(modelFloors.getValueAt(row, 2).toString());

        String capStr = modelFloors.getValueAt(row, 3).toString().replaceAll("[^0-9]", "");
        txtRoomCapacity.setText(capStr);

        btnUpdateFloor.setEnabled(true);
        btnDeleteFloor.setEnabled(true);
    }

    private void clearEditForm() {
        selectedFloorId = -1;
        txtFloorNum.setText("Select a row");
        txtRoomsCount.setText("");
        txtRoomCapacity.setText("");

        btnUpdateFloor.setEnabled(false);
        btnDeleteFloor.setEnabled(false);
        tableFloors.clearSelection();
    }

    private void performUpdateFloor() {
        if (selectedFloorId <= 0) return;

        try {
            int roomsCount   = Integer.parseInt(txtRoomsCount.getText().trim());
            int roomCapacity = Integer.parseInt(txtRoomCapacity.getText().trim());

            blockService.updateFloor(selectedFloorId, roomsCount, roomCapacity);
            showSuccess("Floor properties updated successfully!");

            loadBlockData(selectedBlockName);
            clearEditForm();
        } catch (NumberFormatException ex) {
            showWarning("Rooms count and room capacity must be valid integers.");
        } catch (IllegalArgumentException ex) {
            showWarning(ex.getMessage());
        } catch (Exception ex) {
            showError("Database Error: " + ex.getMessage());
        }
    }

    private void performDeleteFloor() {
        if (selectedFloorId <= 0) return;

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete " + txtFloorNum.getText() + " from Block " + selectedBlockName + "?\n" +
            "Safety checks will be performed to verify no rooms or allocations depend on it.",
            "Confirm Floor Deletion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                blockService.deleteFloor(selectedFloorId);
                showSuccess(txtFloorNum.getText() + " deleted successfully!");
                loadBlockData(selectedBlockName);
                clearEditForm();
            } catch (IllegalArgumentException ex) {
                showWarning(ex.getMessage());
            } catch (Exception ex) {
                showError("Database Error: " + ex.getMessage());
            }
        }
    }

    // ── Utility Helpers ────────────────────────────────────────────────────

    private JButton makeTabButton(String text, String blockName) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(110, 36));
        btn.addActionListener(e -> selectBlock(blockName));
        return btn;
    }

    private JLabel makeStatBadge(String title, String val) {
        JLabel lbl = new JLabel("<html><center><font color='#64748b' size='2'>" + title + "</font><br><font color='#0f172a' size='4'><b>" + val + "</b></font></center></html>");
        lbl.setPreferredSize(new Dimension(85, 42));
        return lbl;
    }

    private void setStatBadgeValue(JLabel lbl, String val) {
        String title = lbl.getText().replaceAll(".*<font color='#64748b' size='2'>(.*?)</font>.*", "$1");
        lbl.setText("<html><center><font color='#64748b' size='2'>" + title + "</font><br><font color='#0f172a' size='4'><b>" + val + "</b></font></center></html>");
    }

    private void addFormField(JPanel parent, String labelText, JComponent field) {
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TEXT_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        parent.add(lbl);
        parent.add(Box.createVerticalStrut(4));
        parent.add(field);
        parent.add(Box.createVerticalStrut(10));
    }

    private JTextField makeTextField() {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 1),
            new EmptyBorder(6, 10, 6, 10)
        ));
        return f;
    }

    private JTextField makeReadOnlyField() {
        JTextField f = makeTextField();
        f.setEditable(false);
        f.setBackground(new Color(0xf1f5f9));
        f.setForeground(TEXT_MUTED);
        return f;
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
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btn.setOpaque(false);
        return btn;
    }

    private void showSuccess(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showWarning(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Validation Warning", JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
