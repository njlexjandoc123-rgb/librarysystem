package com.jandoc.library.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Central blue &amp; white color palette and reusable component styling
 * for the Jandoc Library Management System. Every screen in the app
 * builds its controls through this class, so visual changes made here
 * (button shape, hover feedback, table striping, card framing) apply
 * consistently everywhere instead of being re-implemented per panel.
 */
public class UITheme {

    public static final Color PRIMARY_BLUE   = new Color(21, 67, 168);   // deep header/button blue
    public static final Color PRIMARY_BLUE_HOVER = new Color(27, 82, 199);
    public static final Color ACCENT_BLUE    = new Color(66, 133, 244);  // Google-style accent blue
    public static final Color LIGHT_BLUE     = new Color(232, 240, 254); // panel/background tint
    public static final Color BACKGROUND     = new Color(245, 248, 255); // app background
    public static final Color WHITE          = Color.WHITE;
    public static final Color TEXT_DARK      = new Color(32, 33, 36);
    public static final Color TEXT_GRAY      = new Color(95, 99, 104);
    public static final Color BORDER_GRAY    = new Color(218, 220, 224);
    public static final Color DANGER_RED     = new Color(217, 48, 37);
    public static final Color DANGER_RED_HOVER = new Color(232, 63, 51);
    public static final Color OVERDUE_TINT   = new Color(253, 236, 234);
    public static final Color SUCCESS_GREEN  = new Color(30, 142, 62);
    public static final Color ROW_STRIPE     = new Color(247, 250, 255);

    public static final Font FONT_TITLE   = new Font("SansSerif", Font.BOLD, 24);
    public static final Font FONT_HEADING = new Font("SansSerif", Font.BOLD, 16);
    public static final Font FONT_BODY    = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font FONT_SMALL   = new Font("SansSerif", Font.PLAIN, 12);

    private static final int BUTTON_RADIUS = 10;
    private static final int CARD_RADIUS = 12;

    public static JButton primaryButton(String text) {
        return roundedButton(text, PRIMARY_BLUE, PRIMARY_BLUE_HOVER, WHITE, new EmptyBorder(10, 22, 10, 22));
    }

    public static JButton secondaryButton(String text) {
        JButton btn = roundedButton(text, WHITE, LIGHT_BLUE, PRIMARY_BLUE, new EmptyBorder(9, 20, 9, 20));
        btn.setBorderPainted(false);
        btn.putClientProperty("outline", PRIMARY_BLUE);
        return btn;
    }

    public static JButton dangerButton(String text) {
        return roundedButton(text, DANGER_RED, DANGER_RED_HOVER, WHITE, new EmptyBorder(8, 18, 8, 18));
    }

    /**
     * Builds a flat, rounded-corner button that swaps to {@code hoverColor}
     * on mouse-over. Using painted rounded shapes (instead of the default
     * square platform button) is what gives the app a modern, non-Swing-
     * default look while still being pure Swing with no extra libraries.
     */
    private static JButton roundedButton(String text, Color base, Color hover, Color fg, Border padding) {
        JButton btn = new JButton(text) {
            private boolean hovering = false;

            {
                setContentAreaFilled(false);
                setFocusPainted(false);
                setBorderPainted(false);
                setOpaque(false);
                setBorder(padding);
                setForeground(fg);
                setFont(FONT_BODY);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovering = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hovering = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovering && isEnabled() ? hover : base);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, BUTTON_RADIUS, BUTTON_RADIUS));
                Object outline = getClientProperty("outline");
                if (outline instanceof Color) {
                    g2.setColor((Color) outline);
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 2, getHeight() - 2, BUTTON_RADIUS, BUTTON_RADIUS));
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        return btn;
    }

    public static JTextField styledTextField() {
        JTextField field = new JTextField();
        field.setFont(FONT_BODY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY, 1),
                new EmptyBorder(8, 10, 8, 10)));
        return field;
    }

    public static JPasswordField styledPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(FONT_BODY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY, 1),
                new EmptyBorder(8, 10, 8, 10)));
        return field;
    }

    /**
     * A white, rounded-corner "card" container with a soft border — used to
     * frame forms and content blocks so the layout reads as distinct
     * sections instead of flat, edge-to-edge panels.
     */
    public static JPanel card() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, CARD_RADIUS, CARD_RADIUS));
                g2.setColor(BORDER_GRAY);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 2, getHeight() - 2, CARD_RADIUS, CARD_RADIUS));
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        return panel;
    }

    public static void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setGridColor(BORDER_GRAY);
        table.setSelectionBackground(LIGHT_BLUE);
        table.setSelectionForeground(TEXT_DARK);
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));
        table.getTableHeader().setReorderingAllowed(false);

        // Some Look-and-Feels (notably Windows) ignore setBackground()/
        // setForeground() called directly on JTableHeader and fall back to
        // native per-cell rendering, which left header labels blank/unreadable.
        // Forcing an explicit header cell renderer guarantees the blue
        // background + white bold text shows on every platform.
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                             boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                label.setOpaque(true);
                label.setBackground(PRIMARY_BLUE);
                label.setForeground(WHITE);
                label.setFont(FONT_HEADING.deriveFont(13f));
                label.setHorizontalAlignment(SwingConstants.LEFT);
                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 0, 1, PRIMARY_BLUE_HOVER),
                        new EmptyBorder(0, 10, 0, 10)));
                return label;
            }
        };
        table.getTableHeader().setDefaultRenderer(headerRenderer);

        // Zebra-striped rows read far more easily than a flat white grid.
        DefaultTableCellRenderer stripedRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                             boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? WHITE : ROW_STRIPE);
                    c.setForeground(TEXT_DARK);
                }
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return c;
            }
        };
        table.setDefaultRenderer(Object.class, stripedRenderer);
    }
}
