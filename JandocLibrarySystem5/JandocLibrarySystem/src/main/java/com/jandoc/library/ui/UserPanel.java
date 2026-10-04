package com.jandoc.library.ui;

import com.jandoc.library.service.UserService;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * CRUD interface for the "users" collection: members/borrowers, each
 * classified as Student, Visitor, or Employee.
 */
public class UserPanel extends JPanel {

    private final UserService userService = new UserService();

    private JTextField nameField, emailField, searchField;
    private JPasswordField passwordField;
    private JComboBox<String> roleBox;
    private JTable table;
    private DefaultTableModel tableModel;
    private ObjectId selectedId = null;

    public UserPanel() {
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

        JLabel heading = new JLabel("Members");
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
        String[] columns = {"Name", "Email", "Borrower Type"};
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

        nameField = UITheme.styledTextField();
        emailField = UITheme.styledTextField();
        passwordField = UITheme.styledPasswordField();
        roleBox = new JComboBox<>(new String[]{
                UserService.ROLE_STUDENT, UserService.ROLE_VISITOR, UserService.ROLE_EMPLOYEE
        });
        roleBox.setFont(UITheme.FONT_BODY);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        addFormField(form, c, 0, 0, "Name", nameField);
        addFormField(form, c, 1, 0, "Email", emailField);
        addFormField(form, c, 2, 0, "Password (new only)", passwordField);
        addFormField(form, c, 0, 2, "Borrower Type", roleBox);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        JButton addBtn = UITheme.primaryButton("Add Member");
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

        c.gridx = 1; c.gridy = 2; c.gridwidth = 2;
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
        List<Document> users = userService.getAllUsers(search);
        for (Document u : users) {
            tableModel.addRow(new Object[]{u.getString("name"), u.getString("email"), u.getString("role")});
        }
    }

    private void loadSelectedRowIntoForm() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        String email = (String) tableModel.getValueAt(row, 1);
        List<Document> matches = userService.getAllUsers(email);
        Document user = matches.isEmpty() ? null : matches.get(0);
        if (user == null) return;

        selectedId = user.getObjectId("_id");
        nameField.setText(user.getString("name"));
        emailField.setText(user.getString("email"));
        passwordField.setText("");
        roleBox.setSelectedItem(user.getString("role"));
    }

    private void handleAdd() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String role = (String) roleBox.getSelectedItem();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name, email, and password are required.");
            return;
        }
        boolean success = userService.registerUser(name, email, password, role);
        if (!success) {
            JOptionPane.showMessageDialog(this, "A member with that email already exists.");
            return;
        }
        clearForm();
        refreshTable(null);
    }

    private void handleUpdate() {
        if (selectedId == null) {
            JOptionPane.showMessageDialog(this, "Select a member from the table first.");
            return;
        }
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String newPassword = new String(passwordField.getPassword());
        String role = (String) roleBox.getSelectedItem();

        userService.updateUser(selectedId, name, email, role, newPassword);
        clearForm();
        refreshTable(null);
    }

    private void handleDelete() {
        if (selectedId == null) {
            JOptionPane.showMessageDialog(this, "Select a member from the table first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this member permanently?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            userService.deleteUser(selectedId);
            clearForm();
            refreshTable(null);
        }
    }

    private void clearForm() {
        selectedId = null;
        nameField.setText("");
        emailField.setText("");
        passwordField.setText("");
        roleBox.setSelectedIndex(0);
        table.clearSelection();
    }
}
