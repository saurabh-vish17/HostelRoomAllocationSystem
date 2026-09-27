package com.hostel.ui;

import com.hostel.model.AllocationDetail;
import com.hostel.service.AllocationService;

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
 * VacatePanel — Complete UI for Vacating Hostel Rooms.
 *
 * Requirements:
 *   1. Search active allocations by Student ID, Student Name, or Room Number.
 *   2. Display Student, Room, Block, Floor, Allocation Date, and Current Status.
 *   3. Provide a prominent [VACATE ROOM] action button.
 *   4. Show confirmation dialog before vacating.
 *   5. Preserves historical allocation records (never deletes).
 *   6. Refreshes UI, tables, and statistics after successful vacating.
 */
public class VacatePanel extends JPanel {

    // ── Theme Colors ───────────────────────────────────────────────────────
    private static final Color BG_CANVAS     = UIUtil.BG_DARK;
    private static final Color CARD_BG       = UIUtil.CARD_BG;
    private static final Color CARD_BORDER   = UIUtil.CARD_BORDER;
    private static final Color TEXT_DARK     = UIUtil.TEXT_MAIN;
    private static final Color TEXT_MUTED    = UIUtil.TEXT_MUTED;
    private static final Color ACCENT_RED    = UIUtil.DANGER_RED;
    private static final Color BTN_ROSE      = UIUtil.DANGER_RED;
    private static final Color TABLE_HDR_BG  = UIUtil.BG_DARK;

    // ── Components ─────────────────────────────────────────────────────────
    private JTextField        txtSearch;
    private JComboBox<String> cmbBlockFilter;
    private JComboBox<String> cmbStatusFilter;
    private JButton           btnSearch;
    private JButton           btnResetSearch;
    private JTable            tableVacate;
    private DefaultTableModel modelVacate;
    private JButton           btnVacateRoom;
    private JLabel            lblActiveCount;

    // Selected Allocation Summary Labels
    private JLabel lblSelStudent;
    private JLabel lblSelRoom;
    private JLabel lblSelOccupants;
    private JLabel lblSelAllocDate;

    // ── Service ────────────────────────────────────────────────────────────
    private final AllocationService allocationService = new AllocationService();

    // Currently selected allocation ID
    private AllocationDetail selectedAllocation = null;

    public VacatePanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(BG_CANVAS);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        add(buildHeaderPanel(), BorderLayout.NORTH);
        add(buildMainSplitContent(), BorderLayout.CENTER);

        loadActiveAllocations("");
    }

    // ── Header Panel ───────────────────────────────────────────────────────

    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel title = new JLabel("Room Vacating Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_DARK);

        JLabel sub = new JLabel("Search active allocations, review room occupancy, and process student checkout transactions");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(TEXT_MUTED);

        left.add(title);
        left.add(Box.createVerticalStrut(2));
        left.add(sub);

        lblActiveCount = new JLabel("<html><center><font color='#64748b' size='2'>Active Occupants</font><br><font color='#0f172a' size='4'><b>0</b></font></center></html>");
        lblActiveCount.setPreferredSize(new Dimension(140, 42));

        header.add(left, BorderLayout.WEST);
        header.add(lblActiveCount, BorderLayout.EAST);
        return header;
    }

    // ── Main Content Split (Table Left/Center, Action Panel Right) ─────────

    private JPanel buildMainSplitContent() {
        JPanel container = new JPanel(new BorderLayout(16, 0));
        container.setOpaque(false);

        container.add(buildTableSectionCard(), BorderLayout.CENTER);
        container.add(buildVacateActionCard(), BorderLayout.EAST);

        return container;
    }

    // ── Left/Center Table & Search Section ─────────────────────────────────

    private JPanel buildTableSectionCard() {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 1),
            new EmptyBorder(14, 16, 14, 16)
        ));

        // ── Search & Filter Toolbar ──
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        searchBar.setOpaque(false);

        JLabel lblSearch = new JLabel("🔍 Search:");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSearch.setForeground(TEXT_DARK);

        txtSearch = new JTextField(12);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtSearch.setToolTipText("Search by Student ID, Student Name, or Room Number");

        // Live typing search listener
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { onSearchTriggered(); }
            public void removeUpdate(DocumentEvent e)  { onSearchTriggered(); }
            public void changedUpdate(DocumentEvent e) { onSearchTriggered(); }
        });

        JLabel lblBlock = new JLabel("🏢 Block:");
        lblBlock.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblBlock.setForeground(TEXT_DARK);
        cmbBlockFilter = new JComboBox<>(new String[]{"All Blocks", "Block D", "Block G", "Block H"});
        cmbBlockFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbBlockFilter.addActionListener(e -> onSearchTriggered());

        JLabel lblStatus = new JLabel("📌 Status:");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatus.setForeground(TEXT_DARK);
        cmbStatusFilter = new JComboBox<>(new String[]{"ACTIVE", "VACATED", "ALL"});
        cmbStatusFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbStatusFilter.addActionListener(e -> onSearchTriggered());

        btnSearch = new JButton("Search");
        btnSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnSearch.setPreferredSize(new Dimension(75, 30));
        btnSearch.addActionListener(e -> onSearchTriggered());

        btnResetSearch = new JButton("Reset");
        btnResetSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnResetSearch.setPreferredSize(new Dimension(75, 30));
        btnResetSearch.addActionListener(e -> {
            txtSearch.setText("");
            cmbBlockFilter.setSelectedIndex(0);
            cmbStatusFilter.setSelectedIndex(0);
            onSearchTriggered();
        });

        searchBar.add(lblSearch);
        searchBar.add(txtSearch);
        searchBar.add(lblBlock);
        searchBar.add(cmbBlockFilter);
        searchBar.add(lblStatus);
        searchBar.add(cmbStatusFilter);
        searchBar.add(btnSearch);
        searchBar.add(btnResetSearch);

        card.add(searchBar, BorderLayout.NORTH);

        // ── Allocations Table ──
        String[] cols = {
            "Alloc ID", "Student Roll", "Student Name", "Course & Year",
            "Block", "Floor", "Room Number", "Beds Occupied", "Allocated Date", "Room Status"
        };

        modelVacate = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tableVacate = new JTable(modelVacate);
        tableVacate.setRowHeight(34);
        tableVacate.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableVacate.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableVacate.setGridColor(new Color(0xf1f5f9));
        tableVacate.setSelectionBackground(new Color(0xfee2e2));
        tableVacate.setSelectionForeground(new Color(0x991b1b));

        JTableHeader header = tableVacate.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(TABLE_HDR_BG);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 36));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tableVacate.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // Alloc ID
        tableVacate.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); // Student Roll
        tableVacate.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); // Block
        tableVacate.getColumnModel().getColumn(5).setCellRenderer(centerRenderer); // Floor
        tableVacate.getColumnModel().getColumn(6).setCellRenderer(centerRenderer); // Room Number
        tableVacate.getColumnModel().getColumn(7).setCellRenderer(centerRenderer); // Beds Occupied
        tableVacate.getColumnModel().getColumn(8).setCellRenderer(centerRenderer); // Alloc Date

        // Status Badge Renderer
        tableVacate.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                lbl.setHorizontalAlignment(CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
                if (value != null) {
                    String status = value.toString();
                    if ("AVAILABLE".equals(status)) {
                        lbl.setForeground(new Color(0x15803d));
                    } else if ("PARTIALLY_OCCUPIED".equals(status)) {
                        lbl.setForeground(new Color(0xb45309));
                    } else if ("FULL".equals(status)) {
                        lbl.setForeground(new Color(0xb91c1c));
                    } else if ("MAINTENANCE".equals(status)) {
                        lbl.setForeground(new Color(0x6b7280));
                    }
                }
                return lbl;
            }
        });

        // Row Selection Listener
        tableVacate.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onTableRowSelected();
            }
        });

        JScrollPane scroll = new JScrollPane(tableVacate);
        scroll.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    // ── Right Vacate Action Card ───────────────────────────────────────────

    private JPanel buildVacateActionCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setPreferredSize(new Dimension(340, 0));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 1),
            new EmptyBorder(16, 16, 16, 16)
        ));

        JLabel title = new JLabel("🚪 Room Vacating Action");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(TEXT_DARK);
        title.setBorder(new EmptyBorder(0, 0, 14, 0));
        card.add(title, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Details Panel
        JPanel detailsPanel = new JPanel(new GridLayout(4, 1, 0, 10));
        detailsPanel.setBackground(new Color(0xfff1f2));
        detailsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xfecdd3), 1),
            new EmptyBorder(12, 14, 12, 14)
        ));

        lblSelStudent   = makeDetailLabel("Student", "Select a row from table");
        lblSelRoom      = makeDetailLabel("Room", "-");
        lblSelOccupants = makeDetailLabel("Current Occupancy", "-");
        lblSelAllocDate = makeDetailLabel("Allocated On", "-");

        detailsPanel.add(lblSelStudent);
        detailsPanel.add(lblSelRoom);
        detailsPanel.add(lblSelOccupants);
        detailsPanel.add(lblSelAllocDate);

        content.add(detailsPanel);
        content.add(Box.createVerticalStrut(20));

        // Vacate Info Box
        JTextArea txtInfo = new JTextArea(
            "📌 Note on Vacating:\n" +
            "• Historical allocation data is PRESERVED.\n" +
            "• Vacate date will be set to TODAY.\n" +
            "• Room occupied count will decrease by 1.\n" +
            "• Room status will auto-update to AVAILABLE or PARTIALLY_OCCUPIED."
        );
        txtInfo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        txtInfo.setForeground(TEXT_MUTED);
        txtInfo.setBackground(CARD_BG);
        txtInfo.setEditable(false);
        txtInfo.setLineWrap(true);
        txtInfo.setWrapStyleWord(true);
        txtInfo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 1),
            new EmptyBorder(8, 10, 8, 10)
        ));

        content.add(txtInfo);
        content.add(Box.createVerticalGlue());

        // Vacate Button
        btnVacateRoom = makeCustomButton("🚪 VACATE ROOM", BTN_ROSE, 280);
        btnVacateRoom.setEnabled(false);
        btnVacateRoom.addActionListener(e -> performVacateWorkflow());

        JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnWrapper.setOpaque(false);
        btnWrapper.add(btnVacateRoom);

        content.add(btnWrapper);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ── Table Data & Selection Handlers ────────────────────────────────────

    private void onSearchTriggered() {
        String query = txtSearch.getText().trim();
        loadActiveAllocations(query);
    }

    private void loadActiveAllocations(String query) {
        modelVacate.setRowCount(0);
        selectedAllocation = null;
        updateSelectedDetailsDisplay();

        String selectedBlockStr = (String) cmbBlockFilter.getSelectedItem();
        String blockName = null;
        if (selectedBlockStr != null && selectedBlockStr.startsWith("Block ")) {
            blockName = selectedBlockStr.replace("Block ", "").trim();
        }

        String selectedStatus = (String) cmbStatusFilter.getSelectedItem();

        try {
            List<AllocationDetail> list = allocationService.searchAllocationsFiltered(query, blockName, selectedStatus);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            for (AllocationDetail d : list) {
                modelVacate.addRow(new Object[]{
                    d.getAllocId(),
                    d.getStudentRoll(),
                    d.getStudentName(),
                    d.getCourse() + " (Yr " + d.getYear() + ")",
                    "Block " + d.getBlockName(),
                    "Floor " + d.getFloorNumber(),
                    d.getRoomNumber(),
                    d.getOccupied() + " / " + d.getCapacity(),
                    d.getAllocDate() != null ? sdf.format(d.getAllocDate()) : "-",
                    d.getStatus()
                });
            }

            lblActiveCount.setText(
                "<html><center><font color='#64748b' size='2'>Records Found</font><br><font color='#0f172a' size='4'><b>" +
                list.size() + "</b></font></center></html>"
            );

        } catch (Exception ex) {
            showError("Failed to fetch allocations: " + ex.getMessage());
        }
    }

    private void onTableRowSelected() {
        int row = tableVacate.getSelectedRow();
        if (row >= 0) {
            int allocId = (Integer) modelVacate.getValueAt(row, 0);
            try {
                String query = txtSearch.getText().trim();
                String selectedBlockStr = (String) cmbBlockFilter.getSelectedItem();
                String blockName = null;
                if (selectedBlockStr != null && selectedBlockStr.startsWith("Block ")) {
                    blockName = selectedBlockStr.replace("Block ", "").trim();
                }
                String selectedStatus = (String) cmbStatusFilter.getSelectedItem();

                List<AllocationDetail> list = allocationService.searchAllocationsFiltered(query, blockName, selectedStatus);
                for (AllocationDetail d : list) {
                    if (d.getAllocId() == allocId) {
                        selectedAllocation = d;
                        break;
                    }
                }
            } catch (Exception ex) {
                selectedAllocation = null;
            }
        } else {
            selectedAllocation = null;
        }

        updateSelectedDetailsDisplay();
    }

    private void updateSelectedDetailsDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        if (selectedAllocation != null) {
            lblSelStudent.setText("<html><font size='2' color='#64748b'>Student:</font><br><b>" + selectedAllocation.getStudentName() + " (" + selectedAllocation.getStudentRoll() + ")</b></html>");
            lblSelRoom.setText("<html><font size='2' color='#64748b'>Target Room:</font><br><b>Room " + selectedAllocation.getRoomNumber() + " (Block " + selectedAllocation.getBlockName() + ", Floor " + selectedAllocation.getFloorNumber() + ")</b></html>");
            lblSelOccupants.setText("<html><font size='2' color='#64748b'>Room Occupancy:</font><br><b>" + selectedAllocation.getOccupied() + " / " + selectedAllocation.getCapacity() + " Beds Occupied</b></html>");
            lblSelAllocDate.setText("<html><font size='2' color='#64748b'>Allocated Date:</font><br><b>" + (selectedAllocation.getAllocDate() != null ? sdf.format(selectedAllocation.getAllocDate()) : "-") + "</b></html>");
            btnVacateRoom.setEnabled(true);
        } else {
            lblSelStudent.setText("<html><font size='2' color='#64748b'>Student:</font><br><b>Select a row from table</b></html>");
            lblSelRoom.setText("<html><font size='2' color='#64748b'>Target Room:</font><br><b>-</b></html>");
            lblSelOccupants.setText("<html><font size='2' color='#64748b'>Room Occupancy:</font><br><b>-</b></html>");
            lblSelAllocDate.setText("<html><font size='2' color='#64748b'>Allocated Date:</font><br><b>-</b></html>");
            btnVacateRoom.setEnabled(false);
        }
    }

    // ── Vacate Action Workflow ─────────────────────────────────────────────

    private void performVacateWorkflow() {
        if (selectedAllocation == null) {
            showWarning("Please select an active allocation to vacate.");
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String allocDateStr = selectedAllocation.getAllocDate() != null ? sdf.format(selectedAllocation.getAllocDate()) : "N/A";

        String confirmMsg = String.format(
            "Confirm Room Vacate Operation:\n\n" +
            "  👤 Student: %s (%s)\n" +
            "  🏢 Target Room: Room %s (Block %s, Floor %d)\n" +
            "  📅 Allocated On: %s\n" +
            "  🛏️ Current Occupancy: %d / %d Beds\n\n" +
            "Action Details:\n" +
            "  • Student allocation will be marked as VACATED.\n" +
            "  • Room occupied count will decrease (%d ➡️ %d).\n" +
            "  • Historical allocation record will be safely retained in database.\n\n" +
            "Are you sure you want to proceed with vacating this student?",
            selectedAllocation.getStudentName(), selectedAllocation.getStudentRoll(),
            selectedAllocation.getRoomNumber(), selectedAllocation.getBlockName(), selectedAllocation.getFloorNumber(),
            allocDateStr, selectedAllocation.getOccupied(), selectedAllocation.getCapacity(),
            selectedAllocation.getOccupied(), Math.max(0, selectedAllocation.getOccupied() - 1)
        );

        int confirm = JOptionPane.showConfirmDialog(
            this, confirmMsg, "Confirm Room Vacate", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // Execute Transaction
                allocationService.vacateRoom(selectedAllocation.getAllocId());

                showSuccess("Successfully vacated Room " + selectedAllocation.getRoomNumber() + " for student " + selectedAllocation.getStudentName() + "!");

                // Refresh UI
                loadActiveAllocations(txtSearch.getText().trim());

            } catch (IllegalArgumentException ex) {
                showWarning(ex.getMessage());
            } catch (Exception ex) {
                showError("Vacate Operation Failed: " + ex.getMessage());
            }
        }
    }

    // ── UI Helpers ─────────────────────────────────────────────────────────

    private JLabel makeDetailLabel(String title, String val) {
        JLabel lbl = new JLabel("<html><font size='2' color='#64748b'>" + title + ":</font><br><b>" + val + "</b></html>");
        return lbl;
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
        btn.setPreferredSize(new Dimension(width, 38));
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
