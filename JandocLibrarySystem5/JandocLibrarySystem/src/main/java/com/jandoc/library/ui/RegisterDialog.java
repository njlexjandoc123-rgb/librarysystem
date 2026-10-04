package com.jandoc.library.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.jandoc.library.service.UserService;

/**
 * Registration dialog for creating a new borrower account, including
 * the required borrower classification: Student, Visitor, or Employee.
 */
public class RegisterDialog extends JDialog {

    private final UserService userService = new UserService();

    public RegisterDialog(JFrame parent) {
        super(parent, "Create Account - Miller Library System", true);
        setSize(380, 460);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(UITheme.WHITE);
        setLayout(new BorderLayout());

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(30, 30, 20, 30));
        form.setBackground(UITheme.WHITE);

        JLabel heading = new JLabel("Create your account");
        heading.setFont(UITheme.FONT_HEADING);
        heading.setForeground(UITheme.TEXT_DARK);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField nameField = UITheme.styledTextField();
        JTextField emailField = UITheme.styledTextField();
        JPasswordField passwordField = UITheme.styledPasswordField();
        JComboBox<String> roleBox = new JComboBox<>(new String[]{
                UserService.ROLE_STUDENT, UserService.ROLE_VISITOR, UserService.ROLE_EMPLOYEE
        });
        roleBox.setFont(UITheme.FONT_BODY);
        roleBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        roleBox.setMaximumSize(new Dimension(500, 36));

        form.add(heading);
        form.add(Box.createVerticalStrut(20));
        form.add(labeled("Full name", nameField));
        form.add(Box.createVerticalStrut(12));
        form.add(labeled("Email", emailField));
        form.add(Box.createVerticalStrut(12));
        form.add(labeled("Password", passwordField));
        form.add(Box.createVerticalStrut(12));
        form.add(labeled("Borrower type", roleBox));

        JButton registerBtn = UITheme.primaryButton("Register");
        registerBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        registerBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword());
            String role = (String) roleBox.getSelectedItem();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required.",
                        "Missing information", JOptionPane.WARNING_MESSAGE);
                return;
            }
            boolean success = userService.registerUser(name, email, password, role);
            if (!success) {
                JOptionPane.showMessageDialog(this, "An account with that email already exists.",
                        "Registration failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(this, "Account created! You can now sign in.");
            dispose();
        });

        form.add(Box.createVerticalStrut(24));
        form.add(registerBtn);

        add(form, BorderLayout.CENTER);
    }

    private JPanel labeled(String label, JComponent field) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.FONT_SMALL);
        lbl.setForeground(UITheme.TEXT_GRAY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(500, 36));

        block.add(lbl);
        block.add(Box.createVerticalStrut(4));
        block.add(field);
        return block;
    }
}
