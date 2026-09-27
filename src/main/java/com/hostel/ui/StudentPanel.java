package com.hostel.ui;

import com.hostel.model.Student;
import com.hostel.service.StudentService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * StudentPanel — Complete Student Management UI.
 *
 * Features:
 *   - Form fields: Student ID, Name, Course, Year, Gender, Phone, Email, Address
 *   - Operations: Add, Update, Delete, Search, View All, Clear Form
 *   - JTable display with auto-sort, custom rendering, and row click selection
 *   - Input validation with user-friendly dialogs
 */
public class StudentPanel extends JPanel {

    // ── Palette ────────────────────────────────────────────────────────────
    private static final Color BG_CANVAS     = UIUtil.BG_DARK;
    private static final Color CARD_BG       = UIUtil.CARD_BG;
    private static final Color CARD_BORDER   = UIUtil.CARD_BORDER;
    private static final Color TEXT_DARK     = UIUtil.TEXT_MAIN;
    private static final Color TEXT_MUTED    = UIUtil.TEXT_MUTED;
    private static final Color ACCENT_BLUE   = UIUtil.ACCENT_PRIMARY;
    private static final Color ACCENT_HOVER  = UIUtil.ACCENT_HOVER;
    private static final Color BTN_GREEN     = UIUtil.SUCCESS_GREEN;
    private static final Color BTN_AMBER     = UIUtil.WARNING_AMBER;
    private static final Color BTN_RED       = UIUtil.DANGER_RED;
    private static final Color BTN_SECONDARY = UIUtil.BTN_SECONDARY;
    private static final Color TABLE_HDR_BG  = UIUtil.BG_DARK;

    // ── Form Components ────────────────────────────────────────────────────
    private JTextField     txtStudentId;
    private JTextField     txtName;
    private JTextField     txtCourse;
    private JComboBox<String> cbYear;
    private JComboBox<String> cbGender;
    private JTextField     txtPhone;
    private JTextField     txtEmail;
    private JTextArea      txtAddress;

    private JButton        btnAdd;
    private JButton        btnUpdate;
    private JButton        btnDelete;
    private JButton        btnClear;

    // ── Search & Filter Controls ───────────────────────────────────────────
    private JTextField        txtSearchStudentId;
    private JTextField        txtSearchName;
    private JTextField        txtSearchCourse;
    private JComboBox<String> cmbFilterYear;
    private JButton           btnClearFilters;

    private JTable            table;
    private DefaultTableModel tableModel;
    private JLabel            lblCount;
    private JLabel            lblFormMode;

    // Selected Student internal PK (-1 = none selected)
    private int selectedInternalId = -1;

    private final StudentService studentService = new StudentService();

    public StudentPanel() {
        setLayout(new BorderLayout());
        setBackground(BG_CANVAS);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        add(buildHeaderPanel(), BorderLayout.NORTH);
        add(buildMainSplitView(), BorderLayout.CENTER);

        // Initial table load
        loadStudents(null);
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

        JLabel title = new JLabel("Student Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_DARK);

        JLabel sub = new JLabel("Add, update, search, and dynamically filter registered hostel students");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(TEXT_MUTED);

        left.add(title);
        left.add(Box.createVerticalStrut(2));
        left.add(sub);

        header.add(left, BorderLayout.WEST);
        return header;
    }

    // ── Main Split View (Form Left, Table Right) ───────────────────────────

    private JPanel buildMainSplitView() {
        JPanel split = new JPanel(new BorderLayout(16, 0));
        split.setOpaque(false);

        split.add(buildFormCard(), BorderLayout.WEST);
        split.add(buildTableCard(), BorderLayout.CENTER);

        return split;
    }

    // ── Student Entry Form Card ────────────────────────────────────────────

    private JPanel buildFormCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 1),
            new EmptyBorder(18, 20, 18, 20)
        ));
        card.setPreferredSize(new Dimension(360, 0));

        // Form Title & Mode
        lblFormMode = new JLabel("➕ Add New Student");
        lblFormMode.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblFormMode.setForeground(ACCENT_BLUE);
        lblFormMode.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(lblFormMode);
        card.add(Box.createVerticalStrut(14));

        // Form Fields
        txtStudentId = makeTextField();
        txtName      = makeTextField();
        txtCourse    = makeTextField();
        cbYear       = new JComboBox<>(new String[]{"1", "2", "3", "4", "5"});
        cbGender     = new JComboBox<>(new String[]{"Male", "Female", "Other"});
        txtPhone     = makeTextField();
        txtEmail     = makeTextField();
        txtAddress   = new JTextArea(3, 20);

        formatCombo(cbYear);
        formatCombo(cbGender);

        txtAddress.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtAddress.setLineWrap(true);
        txtAddress.setWrapStyleWord(true);
        JScrollPane scrollAddress = new JScrollPane(txtAddress);
        scrollAddress.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));
        scrollAddress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
        scrollAddress.setAlignmentX(Component.LEFT_ALIGNMENT);

        addFormField(card, "Student ID / Roll No *", txtStudentId);
        addFormField(card, "Full Name *",            txtName);
        addFormField(card, "Course *",               txtCourse);

        // Year & Gender Row
        JPanel row2 = new JPanel(new GridLayout(1, 2, 10, 0));
        row2.setOpaque(false);
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel pYear = new JPanel(new BorderLayout(0, 4));
        pYear.setOpaque(false);
        pYear.add(makeLabel("Year *"), BorderLayout.NORTH);
        pYear.add(cbYear, BorderLayout.CENTER);

        JPanel pGender = new JPanel(new BorderLayout(0, 4));
        pGender.setOpaque(false);
        pGender.add(makeLabel("Gender *"), BorderLayout.NORTH);
        pGender.add(cbGender, BorderLayout.CENTER);

        row2.add(pYear);
        row2.add(pGender);

        card.add(row2);
        card.add(Box.createVerticalStrut(10));

        addFormField(card, "Phone Number *", txtPhone);
        addFormField(card, "Email Address *", txtEmail);

        card.add(makeLabel("Address *"));
        card.add(Box.createVerticalStrut(4));
        card.add(scrollAddress);

        card.add(Box.createVerticalStrut(18));

        // Button Panel
        JPanel btnGrid = new JPanel(new GridLayout(2, 2, 8, 8));
        btnGrid.setOpaque(false);
        btnGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));
        btnGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnAdd    = makeCustomButton("➕ Add", BTN_GREEN, 100);
        btnUpdate = makeCustomButton("✏️ Update", BTN_AMBER, 100);
        btnDelete = makeCustomButton("🗑️ Delete", BTN_RED, 100);
        btnClear  = makeCustomButton("🧹 Clear", BTN_SECONDARY, 100);

        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);

        btnAdd.addActionListener(e -> performAdd());
        btnUpdate.addActionListener(e -> performUpdate());
        btnDelete.addActionListener(e -> performDelete());
        btnClear.addActionListener(e -> clearForm());

        btnGrid.add(btnAdd);
        btnGrid.add(btnUpdate);
        btnGrid.add(btnDelete);
        btnGrid.add(btnClear);

        card.add(btnGrid);
        return card;
    }

    // ── Table Card ─────────────────────────────────────────────────────────

    private JPanel buildTableCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 1),
            new EmptyBorder(12, 12, 12, 12)
        ));

        // Filter Bar (Top of Table Card)
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        filterBar.setOpaque(false);

        JLabel lblId = new JLabel("🆔 Roll ID:");
        lblId.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblId.setForeground(TEXT_DARK);
        txtSearchStudentId = new JTextField(7);
        txtSearchStudentId.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel lblName = new JLabel("👤 Name:");
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblName.setForeground(TEXT_DARK);
        txtSearchName = new JTextField(9);
        txtSearchName.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel lblCourse = new JLabel("📚 Course:");
        lblCourse.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCourse.setForeground(TEXT_DARK);
        txtSearchCourse = new JTextField(8);
        txtSearchCourse.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel lblYear = new JLabel("🎓 Year:");
        lblYear.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblYear.setForeground(TEXT_DARK);
        cmbFilterYear = new JComboBox<>(new String[]{"All Years", "Year 1", "Year 2", "Year 3", "Year 4", "Year 5"});
        cmbFilterYear.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        btnClearFilters = makeCustomButton("🔄 Reset", BTN_SECONDARY, 80);
        btnClearFilters.setPreferredSize(new Dimension(80, 28));
        btnClearFilters.addActionListener(e -> resetSearchFilters());

        DocumentListener docListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { loadStudentsFiltered(); }
            public void removeUpdate(DocumentEvent e)  { loadStudentsFiltered(); }
            public void changedUpdate(DocumentEvent e) { loadStudentsFiltered(); }
        };

        txtSearchStudentId.getDocument().addDocumentListener(docListener);
        txtSearchName.getDocument().addDocumentListener(docListener);
        txtSearchCourse.getDocument().addDocumentListener(docListener);
        cmbFilterYear.addActionListener(e -> loadStudentsFiltered());

        filterBar.add(lblId);
        filterBar.add(txtSearchStudentId);
        filterBar.add(lblName);
        filterBar.add(txtSearchName);
        filterBar.add(lblCourse);
        filterBar.add(txtSearchCourse);
        filterBar.add(lblYear);
        filterBar.add(cmbFilterYear);
        filterBar.add(btnClearFilters);

        card.add(filterBar, BorderLayout.NORTH);

        // Table Model Setup
        String[] cols = {"ID", "Student ID", "Full Name", "Course", "Year", "Gender", "Phone", "Email", "Address"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Table is read-only
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(32);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(new Color(0xf1f5f9));
        table.setSelectionBackground(new Color(0xdbeafe));
        table.setSelectionForeground(new Color(0x1e3a8a));

        // Hide internal PK column (Column 0)
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);

        // Header Styling
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(TABLE_HDR_BG);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 34));

        // Left align table cells
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); // Year
        table.getColumnModel().getColumn(5).setCellRenderer(centerRenderer); // Gender

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));
        card.add(scrollTable, BorderLayout.CENTER);

        // Table Footer Status Bar
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(8, 4, 0, 4));

        lblCount = new JLabel("Total Students: 0");
        lblCount.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCount.setForeground(TEXT_MUTED);

        JLabel hint = new JLabel("💡 Tip: Click any row to select & edit student details");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hint.setForeground(TEXT_MUTED);

        footer.add(lblCount, BorderLayout.WEST);
        footer.add(hint, BorderLayout.EAST);
        card.add(footer, BorderLayout.SOUTH);

        // Row Selection Listener
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                populateFormFromTable(table.getSelectedRow());
            }
        });

        return card;
    }

    // ── Controller Actions ─────────────────────────────────────────────────

    private void loadStudents(String unused) {
        loadStudentsFiltered();
    }

    private void resetSearchFilters() {
        txtSearchStudentId.setText("");
        txtSearchName.setText("");
        txtSearchCourse.setText("");
        cmbFilterYear.setSelectedIndex(0);
        loadStudentsFiltered();
    }

    private void loadStudentsFiltered() {
        tableModel.setRowCount(0);

        String idSearch = txtSearchStudentId != null ? txtSearchStudentId.getText().trim() : "";
        String nameSearch = txtSearchName != null ? txtSearchName.getText().trim() : "";
        String courseSearch = txtSearchCourse != null ? txtSearchCourse.getText().trim() : "";

        Integer yearFilter = null;
        if (cmbFilterYear != null && cmbFilterYear.getSelectedIndex() > 0) {
            yearFilter = cmbFilterYear.getSelectedIndex(); // Index 1 = Year 1, etc.
        }

        try {
            List<Student> list = studentService.searchStudentsFiltered(idSearch, nameSearch, courseSearch, yearFilter);

            for (Student s : list) {
                tableModel.addRow(new Object[]{
                    s.getId(),
                    s.getStudentId(),
                    s.getName(),
                    s.getCourse(),
                    s.getYear(),
                    s.getGender(),
                    s.getPhone(),
                    s.getEmail(),
                    s.getAddress()
                });
            }
            lblCount.setText("Total Students: " + list.size());
        } catch (Exception e) {
            showError("Failed to load students from database:\n" + e.getMessage());
        }
    }

    private void performAdd() {
        try {
            Student s = getStudentFromForm();
            studentService.addStudent(s);

            showSuccess("Student '" + s.getName() + "' added successfully!");
            clearForm();
            loadStudents(null);
        } catch (IllegalArgumentException ex) {
            showWarning(ex.getMessage());
        } catch (Exception ex) {
            showError("Database Error: " + ex.getMessage());
        }
    }

    private void performUpdate() {
        if (selectedInternalId <= 0) {
            showWarning("Please select a student from the table to update.");
            return;
        }

        try {
            Student s = getStudentFromForm();
            s.setId(selectedInternalId);
            studentService.updateStudent(s);

            showSuccess("Student record for '" + s.getName() + "' updated successfully!");
            clearForm();
            loadStudents(null);
        } catch (IllegalArgumentException ex) {
            showWarning(ex.getMessage());
        } catch (Exception ex) {
            showError("Database Error: " + ex.getMessage());
        }
    }

    private void performDelete() {
        if (selectedInternalId <= 0) {
            showWarning("Please select a student from the table to delete.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete student ID '" + txtStudentId.getText() + "' (" + txtName.getText() + ")?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                studentService.deleteStudent(selectedInternalId);
                showSuccess("Student deleted successfully!");
                clearForm();
                loadStudents(null);
            } catch (IllegalArgumentException ex) {
                showWarning(ex.getMessage());
            } catch (Exception ex) {
                showError("Database Error: " + ex.getMessage());
            }
        }
    }

    private void performSearch() {
        loadStudentsFiltered();
    }

    private void populateFormFromTable(int row) {
        selectedInternalId = (int) tableModel.getValueAt(row, 0);
        txtStudentId.setText(tableModel.getValueAt(row, 1).toString());
        txtName.setText(tableModel.getValueAt(row, 2).toString());
        txtCourse.setText(tableModel.getValueAt(row, 3).toString());
        cbYear.setSelectedItem(tableModel.getValueAt(row, 4).toString());
        cbGender.setSelectedItem(tableModel.getValueAt(row, 5).toString());
        txtPhone.setText(tableModel.getValueAt(row, 6).toString());
        txtEmail.setText(tableModel.getValueAt(row, 7).toString());
        txtAddress.setText(tableModel.getValueAt(row, 8).toString());

        lblFormMode.setText("✏️ Edit Student (ID: " + txtStudentId.getText() + ")");
        lblFormMode.setForeground(BTN_AMBER);
        btnAdd.setEnabled(false);
        btnUpdate.setEnabled(true);
        btnDelete.setEnabled(true);
    }

    private void clearForm() {
        selectedInternalId = -1;
        txtStudentId.setText("");
        txtName.setText("");
        txtCourse.setText("");
        cbYear.setSelectedIndex(0);
        cbGender.setSelectedIndex(0);
        txtPhone.setText("");
        txtEmail.setText("");
        txtAddress.setText("");
        table.clearSelection();

        lblFormMode.setText("➕ Add New Student");
        lblFormMode.setForeground(ACCENT_BLUE);
        btnAdd.setEnabled(true);
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);
    }

    private Student getStudentFromForm() {
        String studentId = txtStudentId.getText().trim();
        String name      = txtName.getText().trim();
        String course    = txtCourse.getText().trim();
        int year         = Integer.parseInt((String) cbYear.getSelectedItem());
        String gender    = (String) cbGender.getSelectedItem();
        String phone     = txtPhone.getText().trim();
        String email     = txtEmail.getText().trim();
        String address   = txtAddress.getText().trim();

        return new Student(studentId, name, course, year, gender, phone, email, address);
    }

    // ── Helper Utilities ───────────────────────────────────────────────────

    private void addFormField(JPanel parent, String labelText, JComponent field) {
        parent.add(makeLabel(labelText));
        parent.add(Box.createVerticalStrut(4));
        parent.add(field);
        parent.add(Box.createVerticalStrut(10));
    }

    private JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TEXT_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
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

    private void formatCombo(JComboBox<String> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        combo.setBackground(Color.WHITE);
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
        btn.setPreferredSize(new Dimension(width, 34));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
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
