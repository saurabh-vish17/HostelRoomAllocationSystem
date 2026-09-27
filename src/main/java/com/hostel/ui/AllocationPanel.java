package com.hostel.ui;

import com.hostel.model.AllocationDetail;
import com.hostel.model.RoomDetail;
import com.hostel.model.Student;
import com.hostel.service.AllocationService;
import com.hostel.service.RoomService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * AllocationPanel — Complete UI for Room Allocation Management.
 *
 * UI Allocation Flow:
 *   Select Student → Select Block → Select Floor → Show Available Rooms → Select Room
 *   → Display Capacity, Occupied, Available Beds → Confirmation Dialog → Allocate.
 *
 * Live updates active allocations table and refreshes available bed stats.
 */
public class AllocationPanel extends JPanel {

    // ── Theme Colors ───────────────────────────────────────────────────────
    private static final Color BG_CANVAS     = UIUtil.BG_DARK;
    private static final Color CARD_BG       = UIUtil.CARD_BG;
    private static final Color CARD_BORDER   = UIUtil.CARD_BORDER;
    private static final Color TEXT_DARK     = UIUtil.TEXT_MAIN;
    private static final Color TEXT_MUTED    = UIUtil.TEXT_MUTED;
    private static final Color ACCENT_BLUE   = UIUtil.ACCENT_PRIMARY;
    private static final Color BTN_EMERALD   = UIUtil.SUCCESS_GREEN;
    private static final Color TABLE_HDR_BG  = UIUtil.BG_DARK;

    // ── Form Components ────────────────────────────────────────────────────
    private JComboBox<StudentItem> cmbStudent;
    private JComboBox<String>      cmbBlock;
    private JComboBox<String>      cmbFloor;
    private JComboBox<RoomItem>    cmbRoom;
    private JTextField             txtRemarks;

    // ── Room Info Labels ───────────────────────────────────────────────────
    private JLabel lblCapacityVal;
    private JLabel lblOccupiedVal;
    private JLabel lblAvailableVal;
    private JLabel lblStatusVal;

    // ── Action Buttons ─────────────────────────────────────────────────────
    private JButton btnAllocate;
    private JButton btnClearForm;

    // ── Table Components ───────────────────────────────────────────────────
    private JTable            tableAllocations;
    private DefaultTableModel modelAllocations;
    private JLabel            lblActiveAllocCount;

    // ── Services ───────────────────────────────────────────────────────────
    private final AllocationService allocationService = new AllocationService();
    private final RoomService       roomService       = new RoomService();

    private boolean isUpdatingFloors = false;
    private boolean isUpdatingRooms  = false;

    // Helper item classes for JComboBox binding
    private static class StudentItem {
        final int    dbId;
        final String rollId;
        final String name;
        final String course;

        StudentItem(int dbId, String rollId, String name, String course) {
            this.dbId   = dbId;
            this.rollId = rollId;
            this.name   = name;
            this.course = course;
        }

        @Override
        public String toString() {
            return rollId + " — " + name + " (" + course + ")";
        }
    }

    private static class RoomItem {
        final int    id;
        final String roomNumber;
        final int    capacity;
        final int    occupied;
        final String status;

        RoomItem(int id, String roomNumber, int capacity, int occupied, String status) {
            this.id         = id;
            this.roomNumber = roomNumber;
            this.capacity   = capacity;
            this.occupied   = occupied;
            this.status     = status;
        }

        public int getAvailableBeds() {
            return Math.max(0, capacity - occupied);
        }

        @Override
        public String toString() {
            return roomNumber + "  [" + occupied + "/" + capacity + " beds occupied | " + getAvailableBeds() + " avail]";
        }
    }

    public AllocationPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(BG_CANVAS);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        add(buildHeaderPanel(), BorderLayout.NORTH);
        add(buildMainSplitContent(), BorderLayout.CENTER);

        // Initial Data Load
        loadUnallocatedStudents();
        loadActiveAllocationsTable();
    }

    // ── Header Panel ───────────────────────────────────────────────────────

    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel title = new JLabel("Room Allocation");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_DARK);

        JLabel sub = new JLabel("Assign eligible students to available hostel rooms with real-time capacity verification");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(TEXT_MUTED);

        left.add(title);
        left.add(Box.createVerticalStrut(2));
        left.add(sub);

        lblActiveAllocCount = new JLabel("<html><center><font color='#64748b' size='2'>Active Allocations</font><br><font color='#0f172a' size='4'><b>0</b></font></center></html>");
        lblActiveAllocCount.setPreferredSize(new Dimension(140, 42));

        header.add(left, BorderLayout.WEST);
        header.add(lblActiveAllocCount, BorderLayout.EAST);
        return header;
    }

    // ── Main Content Split (Form Left, Table Right) ────────────────────────

    private JPanel buildMainSplitContent() {
        JPanel container = new JPanel(new BorderLayout(16, 0));
        container.setOpaque(false);

        container.add(buildAllocationFormCard(), BorderLayout.WEST);
        container.add(buildAllocationsTableCard(), BorderLayout.CENTER);

        return container;
    }

    // ── Left Allocation Form Card ──────────────────────────────────────────

    private JPanel buildAllocationFormCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setPreferredSize(new Dimension(380, 0));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 1),
            new EmptyBorder(16, 16, 16, 16)
        ));

        JLabel title = new JLabel("➕ New Allocation Form");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(TEXT_DARK);
        title.setBorder(new EmptyBorder(0, 0, 12, 0));
        card.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(6, 4, 6, 4);
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        int row = 0;

        // 1. Select Student
        form.add(makeFormLabel("1. Select Student (Unallocated):"), gbcRow(gbc, row++));
        cmbStudent = new JComboBox<>();
        cmbStudent.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbStudent.setPreferredSize(new Dimension(0, 32));
        form.add(cmbStudent, gbcRow(gbc, row++));

        // 2. Select Block
        form.add(makeFormLabel("2. Select Block:"), gbcRow(gbc, row++));
        cmbBlock = new JComboBox<>(new String[]{"-- Select Block --", "Block D", "Block G", "Block H"});
        cmbBlock.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbBlock.setPreferredSize(new Dimension(0, 32));
        cmbBlock.addActionListener(e -> onBlockSelected());
        form.add(cmbBlock, gbcRow(gbc, row++));

        // 3. Select Floor
        form.add(makeFormLabel("3. Select Floor:"), gbcRow(gbc, row++));
        cmbFloor = new JComboBox<>(new String[]{"-- Select Floor --"});
        cmbFloor.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbFloor.setPreferredSize(new Dimension(0, 32));
        cmbFloor.addActionListener(e -> {
            if (!isUpdatingFloors) onFloorSelected();
        });
        form.add(cmbFloor, gbcRow(gbc, row++));

        // 4. Select Available Room
        form.add(makeFormLabel("4. Select Available Room:"), gbcRow(gbc, row++));
        cmbRoom = new JComboBox<>();
        cmbRoom.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbRoom.setPreferredSize(new Dimension(0, 32));
        cmbRoom.addActionListener(e -> {
            if (!isUpdatingRooms) updateRoomDetailsDisplay();
        });
        form.add(cmbRoom, gbcRow(gbc, row++));

        // 5. Room Statistics Display Card
        JPanel statsCard = new JPanel(new GridLayout(2, 2, 8, 8));
        statsCard.setBackground(new Color(0xf1f5f9));
        statsCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 1),
            new EmptyBorder(8, 12, 8, 12)
        ));

        lblCapacityVal  = makeStatBox("Capacity", "-");
        lblOccupiedVal  = makeStatBox("Occupied", "-");
        lblAvailableVal = makeStatBox("Available", "-");
        lblStatusVal    = makeStatBox("Status", "-");

        statsCard.add(lblCapacityVal);
        statsCard.add(lblOccupiedVal);
        statsCard.add(lblAvailableVal);
        statsCard.add(lblStatusVal);

        gbcRow(gbc, row++);
        gbc.insets = new java.awt.Insets(10, 4, 10, 4);
        form.add(statsCard, gbc);

        // 6. Remarks Field
        form.add(makeFormLabel("Remarks / Notes (Optional):"), gbcRow(gbc, row++));
        txtRemarks = new JTextField();
        txtRemarks.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtRemarks.setPreferredSize(new Dimension(0, 30));
        form.add(txtRemarks, gbcRow(gbc, row++));

        // 7. Buttons Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        btnClearForm = new JButton("Clear");
        btnClearForm.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClearForm.setPreferredSize(new Dimension(75, 36));
        btnClearForm.addActionListener(e -> resetForm());

        btnAllocate = makeCustomButton("✓ Allocate Room", BTN_EMERALD, 160);
        btnAllocate.addActionListener(e -> performAllocationWorkflow());

        btnPanel.add(btnClearForm);
        btnPanel.add(btnAllocate);

        gbcRow(gbc, row++);
        gbc.insets = new java.awt.Insets(14, 4, 4, 4);
        form.add(btnPanel, gbc);

        card.add(new JScrollPane(form) {
            { setBorder(null); setOpaque(false); getViewport().setOpaque(false); }
        }, BorderLayout.CENTER);

        return card;
    }

    // ── Right Active Allocations Table Card ────────────────────────────────

    private JPanel buildAllocationsTableCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 1),
            new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel title = new JLabel("📋 Active Hostel Allocations");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(TEXT_DARK);
        title.setBorder(new EmptyBorder(0, 4, 10, 4));
        card.add(title, BorderLayout.NORTH);

        String[] cols = {
            "Alloc ID", "Student Roll", "Student Name", "Course",
            "Block", "Floor", "Room Number", "Allocated Date", "Remarks"
        };

        modelAllocations = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tableAllocations = new JTable(modelAllocations);
        tableAllocations.setRowHeight(32);
        tableAllocations.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableAllocations.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableAllocations.setGridColor(new Color(0xf1f5f9));
        tableAllocations.setSelectionBackground(new Color(0xdbeafe));
        tableAllocations.setSelectionForeground(new Color(0x1e3a8a));

        JTableHeader header = tableAllocations.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(TABLE_HDR_BG);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 34));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tableAllocations.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // Alloc ID
        tableAllocations.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); // Roll ID
        tableAllocations.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); // Block
        tableAllocations.getColumnModel().getColumn(5).setCellRenderer(centerRenderer); // Floor
        tableAllocations.getColumnModel().getColumn(6).setCellRenderer(centerRenderer); // Room Number
        tableAllocations.getColumnModel().getColumn(7).setCellRenderer(centerRenderer); // Alloc Date

        JScrollPane scroll = new JScrollPane(tableAllocations);
        scroll.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    // ── Dropdown Handlers & Cascading ──────────────────────────────────────

    private void loadUnallocatedStudents() {
        cmbStudent.removeAllItems();
        try {
            List<Student> unallocated = allocationService.getUnallocatedStudents();
            for (Student s : unallocated) {
                cmbStudent.addItem(new StudentItem(s.getId(), s.getStudentId(), s.getName(), s.getCourse()));
            }
            if (cmbStudent.getItemCount() == 0) {
                cmbStudent.addItem(new StudentItem(-1, "N/A", "No unallocated students available", ""));
                cmbStudent.setEnabled(false);
            } else {
                cmbStudent.setEnabled(true);
            }
        } catch (Exception ex) {
            showError("Failed to fetch unallocated students: " + ex.getMessage());
        }
    }

    private void onBlockSelected() {
        isUpdatingFloors = true;
        cmbFloor.removeAllItems();
        cmbFloor.addItem("-- Select Floor --");

        if (cmbBlock.getSelectedIndex() > 0) {
            String selectedBlock = cmbBlock.getSelectedItem().toString();
            if (selectedBlock.contains("D")) {
                for (int i = 1; i <= 5; i++) cmbFloor.addItem("Floor " + i);
            } else if (selectedBlock.contains("G")) {
                for (int i = 1; i <= 4; i++) cmbFloor.addItem("Floor " + i);
            } else if (selectedBlock.contains("H")) {
                for (int i = 1; i <= 6; i++) cmbFloor.addItem("Floor " + i);
            }
        }

        isUpdatingFloors = false;
        cmbRoom.removeAllItems();
        resetRoomStatsDisplay();
    }

    private void onFloorSelected() {
        isUpdatingRooms = true;
        cmbRoom.removeAllItems();

        if (cmbBlock.getSelectedIndex() > 0 && cmbFloor.getSelectedIndex() > 0) {
            String blockSel = cmbBlock.getSelectedItem().toString().replace("Block ", "");
            String floorSel = cmbFloor.getSelectedItem().toString().replace("Floor ", "");
            Integer floorNum = Integer.parseInt(floorSel);

            try {
                // Fetch rooms filtered by Block & Floor that are AVAILABLE or PARTIALLY_OCCUPIED
                List<RoomDetail> availableRooms = roomService.searchAndFilterRooms(blockSel, floorNum, null, null);

                for (RoomDetail r : availableRooms) {
                    if (!"MAINTENANCE".equalsIgnoreCase(r.getStatus()) && r.getOccupied() < r.getCapacity()) {
                        cmbRoom.addItem(new RoomItem(r.getId(), r.getRoomNumber(), r.getCapacity(), r.getOccupied(), r.getStatus()));
                    }
                }

                if (cmbRoom.getItemCount() == 0) {
                    cmbRoom.addItem(new RoomItem(-1, "No Available Rooms", 0, 0, "FULL"));
                    cmbRoom.setEnabled(false);
                } else {
                    cmbRoom.setEnabled(true);
                }
            } catch (Exception ex) {
                showError("Failed to fetch available rooms: " + ex.getMessage());
            }
        }

        isUpdatingRooms = false;
        updateRoomDetailsDisplay();
    }

    private void updateRoomDetailsDisplay() {
        Object item = cmbRoom.getSelectedItem();
        if (item instanceof RoomItem room && room.id > 0) {
            lblCapacityVal.setText("<html><center><font size='1' color='#64748b'>Capacity</font><br><b>" + room.capacity + " Beds</b></center></html>");
            lblOccupiedVal.setText("<html><center><font size='1' color='#64748b'>Occupied</font><br><b>" + room.occupied + "</b></center></html>");
            lblAvailableVal.setText("<html><center><font size='1' color='#15803d'>Available</font><br><font color='#15803d'><b>" + room.getAvailableBeds() + " Beds</b></font></center></html>");
            lblStatusVal.setText("<html><center><font size='1' color='#64748b'>Status</font><br><b>" + room.status + "</b></center></html>");
        } else {
            resetRoomStatsDisplay();
        }
    }

    private void resetRoomStatsDisplay() {
        lblCapacityVal.setText("<html><center><font size='1' color='#64748b'>Capacity</font><br><b>-</b></center></html>");
        lblOccupiedVal.setText("<html><center><font size='1' color='#64748b'>Occupied</font><br><b>-</b></center></html>");
        lblAvailableVal.setText("<html><center><font size='1' color='#64748b'>Available</font><br><b>-</b></center></html>");
        lblStatusVal.setText("<html><center><font size='1' color='#64748b'>Status</font><br><b>-</b></center></html>");
    }

    // ── Allocation Action Workflow ─────────────────────────────────────────

    private void performAllocationWorkflow() {
        // 1. Validate Student Selection
        Object studentObj = cmbStudent.getSelectedItem();
        if (!(studentObj instanceof StudentItem student) || student.dbId <= 0) {
            showWarning("Please select an eligible unallocated student.");
            return;
        }

        // 2. Validate Block & Floor
        if (cmbBlock.getSelectedIndex() <= 0) {
            showWarning("Please select a hostel block.");
            return;
        }
        if (cmbFloor.getSelectedIndex() <= 0) {
            showWarning("Please select a floor.");
            return;
        }

        // 3. Validate Room Selection
        Object roomObj = cmbRoom.getSelectedItem();
        if (!(roomObj instanceof RoomItem room) || room.id <= 0) {
            showWarning("Please select an available room.");
            return;
        }

        if (room.getAvailableBeds() <= 0) {
            showWarning("Selected Room " + room.roomNumber + " has no available beds.");
            return;
        }

        String remarks = txtRemarks.getText().trim();

        // 4. Prompt Confirmation Dialog
        String confirmMsg = String.format(
            "Confirm Room Allocation:\n\n" +
            "  👤 Student: %s (%s)\n" +
            "  🏢 Target Room: Room %s (%s, %s)\n" +
            "  🛏️ Current Occupancy: %d / %d Beds\n" +
            "  ➡️ New Occupancy: %d / %d Beds (%d Available Bed Remaining)\n\n" +
            "Proceed with this allocation?",
            student.name, student.rollId,
            room.roomNumber, cmbBlock.getSelectedItem(), cmbFloor.getSelectedItem(),
            room.occupied, room.capacity,
            room.occupied + 1, room.capacity, room.getAvailableBeds() - 1
        );

        int confirm = JOptionPane.showConfirmDialog(
            this, confirmMsg, "Confirm Room Allocation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // Execute Transaction
                allocationService.allocateRoom(student.dbId, room.id, remarks);
                showSuccess("Successfully allocated Room " + room.roomNumber + " to " + student.name + "!");

                // Refresh UI
                resetForm();
                loadUnallocatedStudents();
                loadActiveAllocationsTable();

            } catch (IllegalArgumentException ex) {
                showWarning(ex.getMessage());
            } catch (Exception ex) {
                showError("Allocation Transaction Failed: " + ex.getMessage());
            }
        }
    }

    private void loadActiveAllocationsTable() {
        modelAllocations.setRowCount(0);
        try {
            List<AllocationDetail> activeList = allocationService.getAllActiveAllocations();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            for (AllocationDetail d : activeList) {
                modelAllocations.addRow(new Object[]{
                    d.getAllocId(),
                    d.getStudentRoll(),
                    d.getStudentName(),
                    d.getCourse() + " (Yr " + d.getYear() + ")",
                    "Block " + d.getBlockName(),
                    "Floor " + d.getFloorNumber(),
                    d.getRoomNumber(),
                    d.getAllocDate() != null ? sdf.format(d.getAllocDate()) : "-",
                    d.getRemarks() != null ? d.getRemarks() : ""
                });
            }

            // Update Header Count Badge
            lblActiveAllocCount.setText(
                "<html><center><font color='#64748b' size='2'>Active Allocations</font><br><font color='#0f172a' size='4'><b>" +
                activeList.size() + "</b></font></center></html>"
            );

        } catch (Exception ex) {
            showError("Failed to fetch active allocations table: " + ex.getMessage());
        }
    }

    private void resetForm() {
        if (cmbStudent.getItemCount() > 0) cmbStudent.setSelectedIndex(0);
        cmbBlock.setSelectedIndex(0);
        cmbFloor.removeAllItems();
        cmbFloor.addItem("-- Select Floor --");
        cmbRoom.removeAllItems();
        txtRemarks.setText("");
        resetRoomStatsDisplay();
    }

    // ── UI Component Factories & Helpers ───────────────────────────────────

    private JLabel makeFormLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(TEXT_DARK);
        return lbl;
    }

    private JLabel makeStatBox(String title, String initialVal) {
        JLabel lbl = new JLabel("<html><center><font size='1' color='#64748b'>" + title + "</font><br><b>" + initialVal + "</b></center></html>");
        lbl.setHorizontalAlignment(JLabel.CENTER);
        return lbl;
    }

    private GridBagConstraints gbcRow(GridBagConstraints gbc, int row) {
        gbc.gridy = row;
        return gbc;
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
