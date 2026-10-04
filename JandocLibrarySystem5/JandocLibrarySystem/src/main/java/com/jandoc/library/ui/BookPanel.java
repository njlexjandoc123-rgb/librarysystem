package com.jandoc.library.ui;

import com.jandoc.library.service.BookService;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * CRUD interface for the "books" collection: add, search, update, and
 * delete books, with live tracking of total vs. available copies.
 */
public class BookPanel extends JPanel {

    private final BookService bookService = new BookService();

    private JTextField titleField, authorField, isbnField, categoryField, totalCopiesField;
    private JTextField searchField;
    private JTable table;
    private DefaultTableModel tableModel;
    private ObjectId selectedId = null;

    public BookPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        add(buildSearchBar(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildForm(), BorderLayout.SOUTH);

        refreshTable(null);
    }

    private JPanel buildSearchBar() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);

        searchField = UITheme.styledTextField();
        JButton searchBtn = UITheme.secondaryButton("Search");
        searchBtn.addActionListener(e -> refreshTable(searchField.getText().trim()));

        JLabel heading = new JLabel("Books");
        heading.setFont(UITheme.FONT_HEADING);
        heading.setForeground(UITheme.TEXT_DARK);

        JPanel searchWrap = new JPanel(new BorderLayout(8, 0));
        searchWrap.setOpaque(false);
        searchWrap.add(searchField, BorderLayout.CENTER);
        searchWrap.add(searchBtn, BorderLayout.EAST);

        panel.add(heading, BorderLayout.WEST);
        panel.add(searchWrap, BorderLayout.EAST);
        return panel;
    }

    private JScrollPane buildTable() {
        String[] columns = {"Title", "Author", "ISBN", "Category", "Total", "Available"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(tableModel);
        UITheme.styleTable(table);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadSelectedRowIntoForm();
        });
        return new JScrollPane(table);
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_GRAY),
                new EmptyBorder(14, 14, 14, 14)));

        titleField = UITheme.styledTextField();
        authorField = UITheme.styledTextField();
        isbnField = UITheme.styledTextField();
        categoryField = UITheme.styledTextField();
        totalCopiesField = UITheme.styledTextField();

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        addFormField(form, c, 0, 0, "Title", titleField);
        addFormField(form, c, 1, 0, "Author", authorField);
        addFormField(form, c, 2, 0, "ISBN", isbnField);
        addFormField(form, c, 0, 2, "Category", categoryField);
        addFormField(form, c, 1, 2, "Total Copies", totalCopiesField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        JButton addBtn = UITheme.primaryButton("Add Book");
        JButton updateBtn = UITheme.secondaryButton("Update");
        JButton deleteBtn = UITheme.dangerButton("Delete");
        JButton clearBtn = UITheme.secondaryButton("Clear");

        addBtn.addActionListener(e -> handleAdd());
        updateBtn.addActionListener(e -> handleUpdate());
        deleteBtn.addActionListener(e -> handleDelete());
        clearBtn.addActionListener(e -> clearForm());

        buttons.add(addBtn);
        buttons.add(updateBtn);
        buttons.add(deleteBtn);
        buttons.add(clearBtn);

        c.gridx = 2; c.gridy = 2; c.gridwidth = 1;
        form.add(buttons, c);

        return form;
    }

    private void addFormField(JPanel form, GridBagConstraints c, int gridx, int gridy, String label, JComponent field) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.FONT_SMALL);
        lbl.setForeground(UITheme.TEXT_GRAY);

        c.gridx = gridx; c.gridy = gridy; c.gridwidth = 1;
        form.add(lbl, c);
        c.gridy = gridy + 1;
        field.setPreferredSize(new Dimension(180, 32));
        form.add(field, c);
    }

    private void refreshTable(String search) {
        tableModel.setRowCount(0);
        List<Document> books = bookService.getAllBooks(search);
        for (Document b : books) {
            tableModel.addRow(new Object[]{
                    b.getString("title"), b.getString("author"), b.getString("isbn"),
                    b.getString("category"), b.getInteger("totalCopies"), b.getInteger("availableCopies")
            });
        }
    }

    private void loadSelectedRowIntoForm() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        String isbn = (String) tableModel.getValueAt(row, 2);
        Document book = bookService.findBookByIsbn(isbn);
        if (book == null) return;

        selectedId = book.getObjectId("_id");
        titleField.setText(book.getString("title"));
        authorField.setText(book.getString("author"));
        isbnField.setText(book.getString("isbn"));
        categoryField.setText(book.getString("category"));
        totalCopiesField.setText(String.valueOf(book.getInteger("totalCopies")));
    }

    private void handleAdd() {
        try {
            String title = titleField.getText().trim();
            String author = authorField.getText().trim();
            String isbn = isbnField.getText().trim();
            String category = categoryField.getText().trim();
            int total = Integer.parseInt(totalCopiesField.getText().trim());

            if (title.isEmpty() || isbn.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Title and ISBN are required.");
                return;
            }
            bookService.addBook(title, author, isbn, category, total);
            clearForm();
            refreshTable(null);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Total copies must be a number.");
        }
    }

    private void handleUpdate() {
        if (selectedId == null) {
            JOptionPane.showMessageDialog(this, "Select a book from the table first.");
            return;
        }
        try {
            int total = Integer.parseInt(totalCopiesField.getText().trim());
            Document existing = bookService.findBookByIsbn(isbnField.getText().trim());
            int available = existing != null ? existing.getInteger("availableCopies") : total;
            bookService.updateBook(selectedId, titleField.getText().trim(), authorField.getText().trim(),
                    categoryField.getText().trim(), total, Math.min(available, total));
            clearForm();
            refreshTable(null);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Total copies must be a number.");
        }
    }

    private void handleDelete() {
        if (selectedId == null) {
            JOptionPane.showMessageDialog(this, "Select a book from the table first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this book permanently?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            bookService.deleteBook(selectedId);
            clearForm();
            refreshTable(null);
        }
    }

    private void clearForm() {
        selectedId = null;
        titleField.setText("");
        authorField.setText("");
        isbnField.setText("");
        categoryField.setText("");
        totalCopiesField.setText("");
        table.clearSelection();
    }
}
