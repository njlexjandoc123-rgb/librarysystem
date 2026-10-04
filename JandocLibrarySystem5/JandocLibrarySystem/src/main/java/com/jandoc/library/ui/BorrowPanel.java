package com.jandoc.library.ui;

import com.jandoc.library.exception.LibraryException;
import com.jandoc.library.service.LibraryService;
import com.jandoc.library.service.UserService;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Borrow / Return tab: pick a member, borrow a book by ISBN, and view
 * or return that member's currently active borrowed books.
 */
public class BorrowPanel extends JPanel {

    private final LibraryService libraryService = new LibraryService();
    private final UserService userService = new UserService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy");

    private static final String CARD_TABLE = "table";
    private static final String CARD_EMPTY = "empty";

    private JComboBox<UserItem> memberBox;
    private JTextField isbnField;
    private JTable activeTable;
    private DefaultTableModel activeModel;
    private JPanel tableCardPanel;
    private JLabel emptyStateLabel;

    public BorrowPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        add(buildTopForm(), BorderLayout.NORTH);
        add(buildActiveTable(), BorderLayout.CENTER);

        refreshMembers();
    }

    private JPanel buildTopForm() {
        JPanel panel = UITheme.card();
        panel.setLayout(new GridBagLayout());

        memberBox = new JComboBox<>();
        memberBox.setFont(UITheme.FONT_BODY);
        memberBox.addActionListener(e -> refreshActiveTable());

        isbnField = UITheme.styledTextField();

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel memberLbl = new JLabel("MEMBER");
        memberLbl.setFont(UITheme.FONT_SMALL);
        memberLbl.setForeground(UITheme.TEXT_GRAY);
        c.gridx = 0; c.gridy = 0; panel.add(memberLbl, c);
        c.gridy = 1; memberBox.setPreferredSize(new Dimension(220, 32)); panel.add(memberBox, c);

        JLabel isbnLbl = new JLabel("BOOK ISBN");
        isbnLbl.setFont(UITheme.FONT_SMALL);
        isbnLbl.setForeground(UITheme.TEXT_GRAY);
        c.gridx = 1; c.gridy = 0; panel.add(isbnLbl, c);
        c.gridy = 1; isbnField.setPreferredSize(new Dimension(180, 32)); panel.add(isbnField, c);

        JButton borrowBtn = UITheme.primaryButton("Borrow Book");
        borrowBtn.addActionListener(e -> handleBorrow());
        c.gridx = 2; c.gridy = 1; panel.add(borrowBtn, c);

        JButton refreshBtn = UITheme.secondaryButton("Refresh Members");
        refreshBtn.addActionListener(e -> refreshMembers());
        c.gridx = 3; c.gridy = 1; panel.add(refreshBtn, c);

        return panel;
    }

    private JPanel buildActiveTable() {
        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);

        JLabel heading = new JLabel("Active borrows for selected member");
        heading.setFont(UITheme.FONT_HEADING.deriveFont(14f));
        heading.setForeground(UITheme.TEXT_DARK);

        String[] columns = {"Title", "ISBN", "Borrow Date", "Due Date", "Status"};
        activeModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        activeTable = new JTable(activeModel);
        UITheme.styleTable(activeTable);
        applyStatusColumnStyling();

        JButton returnBtn = UITheme.secondaryButton("Return Selected Book");
        returnBtn.addActionListener(e -> handleReturn());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.setOpaque(false);
        bottom.add(returnBtn);

        // Shown in place of the table when the selected member has zero
        // active borrows, so an empty result reads as "nothing borrowed"
        // instead of looking like the app failed to load anything.
        JLabel emptyState = new JLabel("No active borrows for this member.", SwingConstants.CENTER);
        emptyState.setFont(UITheme.FONT_BODY);
        emptyState.setForeground(UITheme.TEXT_GRAY);
        emptyState.setOpaque(true);
        emptyState.setBackground(UITheme.WHITE);
        emptyState.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_GRAY, 1));
        this.emptyStateLabel = emptyState;

        JScrollPane scroll = new JScrollPane(activeTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_GRAY, 1));
        scroll.getViewport().setBackground(UITheme.WHITE);

        tableCardPanel = new JPanel(new CardLayout());
        tableCardPanel.setOpaque(false);
        tableCardPanel.add(scroll, CARD_TABLE);
        tableCardPanel.add(emptyState, CARD_EMPTY);

        wrap.add(heading, BorderLayout.NORTH);
        wrap.add(tableCardPanel, BorderLayout.CENTER);
        wrap.add(bottom, BorderLayout.SOUTH);
        return wrap;
    }

    /**
     * Colors the "Status" column and tints overdue rows so a librarian can
     * spot late books at a glance instead of comparing dates manually.
     */
    private void applyStatusColumnStyling() {
        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                             boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                boolean overdue = "Overdue".equals(value);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                setFont(UITheme.FONT_BODY.deriveFont(Font.BOLD));
                if (!isSelected) {
                    c.setBackground(overdue ? UITheme.OVERDUE_TINT : (row % 2 == 0 ? UITheme.WHITE : UITheme.ROW_STRIPE));
                }
                c.setForeground(overdue ? UITheme.DANGER_RED : UITheme.SUCCESS_GREEN);
                return c;
            }
        };
        activeTable.getColumnModel().getColumn(4).setCellRenderer(statusRenderer);
        activeTable.getColumnModel().getColumn(4).setPreferredWidth(80);
    }

    private void refreshMembers() {
        memberBox.removeAllItems();
        List<Document> users = userService.getAllUsers(null);
        for (Document u : users) {
            memberBox.addItem(new UserItem(u.getObjectId("_id"), u.getString("name") + " (" + u.getString("role") + ")"));
        }
        refreshActiveTable();
    }

    private void refreshActiveTable() {
        activeModel.setRowCount(0);
        UserItem selected = (UserItem) memberBox.getSelectedItem();

        if (selected == null) {
            emptyStateLabel.setText("No members found. Register a member first.");
            showCard(CARD_EMPTY);
            return;
        }

        Date now = new Date();
        List<Document> active = libraryService.getActiveBorrows(selected.id);
        for (Document entry : active) {
            Date due = entry.getDate("dueDate");
            String borrowDate = entry.getDate("borrowDate") != null ? dateFormat.format(entry.getDate("borrowDate")) : "";
            String dueDate = due != null ? dateFormat.format(due) : "";
            String status = (due != null && due.before(now)) ? "Overdue" : "Active";
            activeModel.addRow(new Object[]{entry.getString("title"), entry.getString("isbn"), borrowDate, dueDate, status});
        }

        if (active.isEmpty()) {
            emptyStateLabel.setText("No active borrows for " + selected.label + ".");
            showCard(CARD_EMPTY);
        } else {
            showCard(CARD_TABLE);
        }
    }

    private void showCard(String cardName) {
        ((CardLayout) tableCardPanel.getLayout()).show(tableCardPanel, cardName);
    }

    private void handleBorrow() {
        UserItem selected = (UserItem) memberBox.getSelectedItem();
        String isbn = isbnField.getText().trim();
        if (selected == null) {
            showWarning("Select a member first.");
            return;
        }
        if (isbn.isEmpty()) {
            showWarning("Enter a book ISBN.");
            return;
        }

        try {
            libraryService.borrowBook(selected.id, isbn);
            showInfo("Book borrowed successfully!");
            isbnField.setText("");
        } catch (LibraryException ex) {
            showWarning(ex.getMessage());
        }
        refreshActiveTable();
    }

    private void handleReturn() {
        UserItem selected = (UserItem) memberBox.getSelectedItem();
        int row = activeTable.getSelectedRow();
        if (selected == null || row < 0) {
            showWarning("Select a member and a book row to return.");
            return;
        }
        String isbn = (String) activeModel.getValueAt(row, 1);

        try {
            libraryService.returnBook(selected.id, isbn);
            showInfo("Book returned successfully!");
        } catch (LibraryException ex) {
            showWarning(ex.getMessage());
        }
        refreshActiveTable();
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Unable to complete", JOptionPane.WARNING_MESSAGE);
    }

    /** Wraps a user's ObjectId with a friendly display label for the combo box. */
    private static class UserItem {
        final ObjectId id;
        final String label;

        UserItem(ObjectId id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() { return label; }
    }
}
