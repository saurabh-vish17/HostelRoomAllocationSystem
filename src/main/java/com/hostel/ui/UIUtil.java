package com.hostel.ui;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * UIUtil — Central UI Theme, Color Palette, Typography, Table Renderers, and Dialog Helpers.
 * Enforces a modern dark slate aesthetic across all application panels.
 */
public class UIUtil {

    // ── Modern Palette ──────────────────────────────────────────────────────
    public static final Color BG_DARK        = new Color(0x0f172a); // Slate 900
    public static final Color CARD_BG        = new Color(0x1e293b); // Slate 800
    public static final Color CARD_BG_LIGHT  = new Color(0x334155); // Slate 700
    public static final Color CARD_BORDER    = new Color(0x334155);
    public static final Color BORDER_SUBTLE  = new Color(0x1e293b);

    public static final Color ACCENT_PRIMARY = new Color(0x3b82f6); // Vibrant Blue
    public static final Color ACCENT_HOVER   = new Color(0x2563eb);
    public static final Color SUCCESS_GREEN  = new Color(0x10b981); // Emerald Green
    public static final Color SUCCESS_HOVER  = new Color(0x059669);
    public static final Color WARNING_AMBER  = new Color(0xf59e0b); // Amber
    public static final Color WARNING_HOVER  = new Color(0xd97706);
    public static final Color DANGER_RED     = new Color(0xef4444); // Coral Red
    public static final Color DANGER_HOVER   = new Color(0xdc2626);
    public static final Color BTN_SECONDARY  = new Color(0x334155);
    public static final Color BTN_2ND_HOVER  = new Color(0x475569);

    public static final Color TEXT_MAIN      = new Color(0xf8fafc); // Slate 50
    public static final Color TEXT_MUTED     = new Color(0x94a3b8); // Slate 400
    public static final Color TEXT_DARK      = new Color(0xe2e8f0);

    // ── Typography ─────────────────────────────────────────────────────────
    public static final Font FONT_HERO       = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font FONT_TITLE      = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_SECTION    = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_SUBTITLE   = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY       = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD       = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL      = new Font("Segoe UI", Font.PLAIN, 12);

    /**
     * Initializes FlatLaf Look and Feel and UI defaults.
     */
    public static void setupTheme() {
        try {
            FlatDarkLaf.setup();
            UIManager.put("Component.arc", 10);
            UIManager.put("Button.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("Table.rowHeight", 38);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false);
            UIManager.put("TableHeader.height", 40);
            UIManager.put("TableHeader.separatorColor", CARD_BORDER);
            UIManager.put("TableHeader.background", new Color(0x0f172a));
            UIManager.put("TableHeader.foreground", TEXT_MUTED);
            UIManager.put("TableHeader.font", new Font("Segoe UI", Font.BOLD, 13));
            UIManager.put("Table.background", CARD_BG);
            UIManager.put("Table.foreground", TEXT_MAIN);
            UIManager.put("Table.gridColor", new Color(0x334155));
            UIManager.put("Table.selectionBackground", new Color(0x1e3a8a));
            UIManager.put("Table.selectionForeground", TEXT_MAIN);
            UIManager.put("Panel.background", BG_DARK);
            UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(CARD_BORDER, 1));
            UIManager.put("TabbedPane.selectedBackground", CARD_BG);
            UIManager.put("TabbedPane.selectedForeground", ACCENT_PRIMARY);
            UIManager.put("TabbedPane.font", FONT_BOLD);
        } catch (Exception e) {
            System.err.println("[UIUtil] Failed to set FlatLaf: " + e.getMessage());
        }
    }

    /**
     * Helper to create styled card containers.
     */
    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 1),
            new EmptyBorder(16, 16, 16, 16)
        ));
        return panel;
    }

    /**
     * Helper to apply modern header formatting to a JTable.
     */
    public static void styleTable(JTable table) {
        table.setRowHeight(38);
        table.setIntercellSpacing(new Dimension(8, 0));
        table.setFillsViewportHeight(true);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(0x334155));

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(new Color(0x0f172a));
        header.setForeground(TEXT_MUTED);
        header.setReorderingAllowed(false);

        // Center or left-align default renderer
        DefaultTableCellRenderer renderer = (DefaultTableCellRenderer) header.getDefaultRenderer();
        renderer.setHorizontalAlignment(SwingConstants.LEFT);
    }

    /**
     * Custom Table Renderer for Status Pills (AVAILABLE, FULL, PARTIALLY_OCCUPIED, MAINTENANCE, ACTIVE, VACATED).
     */
    public static class StatusPillCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String status = value != null ? value.toString().trim() : "";

            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 12));
            label.setOpaque(true);

            if (isSelected) {
                label.setBackground(table.getSelectionBackground());
                label.setForeground(table.getSelectionForeground());
                return label;
            }

            switch (status.toUpperCase()) {
                case "AVAILABLE":
                    label.setBackground(new Color(0x065f46)); // dark emerald
                    label.setForeground(new Color(0x6ee7b7));
                    label.setText("🟢 AVAILABLE");
                    break;
                case "PARTIALLY_OCCUPIED":
                    label.setBackground(new Color(0x78350f)); // dark amber
                    label.setForeground(new Color(0xfcd34d));
                    label.setText("🟡 PARTIAL");
                    break;
                case "FULL":
                    label.setBackground(new Color(0x7f1d1d)); // dark red
                    label.setForeground(new Color(0xfca5a5));
                    label.setText("🔴 FULL");
                    break;
                case "MAINTENANCE":
                    label.setBackground(new Color(0x581c87)); // dark purple
                    label.setForeground(new Color(0xd8b4fe));
                    label.setText("🔧 MAINTENANCE");
                    break;
                case "ACTIVE":
                    label.setBackground(new Color(0x1e3a8a)); // dark blue
                    label.setForeground(new Color(0x93c5fd));
                    label.setText("🟢 ACTIVE");
                    break;
                case "VACATED":
                    label.setBackground(new Color(0x334155)); // dark slate
                    label.setForeground(new Color(0xcbd5e1));
                    label.setText("⚪ VACATED");
                    break;
                default:
                    label.setBackground(new Color(0x1e293b));
                    label.setForeground(TEXT_MAIN);
                    break;
            }

            label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            return label;
        }
    }

    /**
     * Dialog Helpers with styled message popups.
     */
    public static void showSuccess(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent,
            "<html><body style='width: 300px; padding: 6px; font-family: Segoe UI; color: #f8fafc;'>" +
            "<h4 style='margin:0 0 6px 0; color: #10b981;'>✔ " + title + "</h4>" +
            "<div>" + message.replace("\n", "<br>") + "</div></body></html>",
            title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showError(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent,
            "<html><body style='width: 320px; padding: 6px; font-family: Segoe UI; color: #f8fafc;'>" +
            "<h4 style='margin:0 0 6px 0; color: #ef4444;'>❌ " + title + "</h4>" +
            "<div>" + message.replace("\n", "<br>") + "</div></body></html>",
            title, JOptionPane.ERROR_MESSAGE);
    }

    public static void showWarning(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent,
            "<html><body style='width: 320px; padding: 6px; font-family: Segoe UI; color: #f8fafc;'>" +
            "<h4 style='margin:0 0 6px 0; color: #f59e0b;'>⚠️ " + title + "</h4>" +
            "<div>" + message.replace("\n", "<br>") + "</div></body></html>",
            title, JOptionPane.WARNING_MESSAGE);
    }

    public static boolean showConfirm(Component parent, String title, String message) {
        int choice = JOptionPane.showConfirmDialog(parent,
            "<html><body style='width: 320px; padding: 6px; font-family: Segoe UI; color: #f8fafc;'>" +
            "<h4 style='margin:0 0 6px 0; color: #3b82f6;'>❓ " + title + "</h4>" +
            "<div>" + message.replace("\n", "<br>") + "</div></body></html>",
            title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }
}
