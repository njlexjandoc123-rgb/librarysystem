package com.jandoc.library.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.bson.Document;

import com.jandoc.library.service.UserService;

/**
 * Login screen styled after the clean, card-centered look of a Google
 * account sign-in page: a white rounded card floating on a light
 * background, a circular avatar/brand mark, and a single accent-blue
 * primary action button.
 */
public class LoginFrame extends JFrame {

    private final UserService userService = new UserService();

    private JTextField emailField;
    private JPasswordField passwordField;

    public LoginFrame() {
        setTitle("Sign in - Miller Library Management System");
        setSize(420, 600);
        setMinimumSize(new Dimension(380, 560));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        getContentPane().setBackground(UITheme.BACKGROUND);
        setLayout(new GridBagLayout());
        add(buildCard());

        getRootPane().setDefaultButton(null);
        setVisible(true);
    }

    private JPanel buildCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UITheme.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_GRAY, 1),
                new EmptyBorder(40, 40, 30, 40)));
        card.setPreferredSize(new Dimension(360, 520));
        card.setMaximumSize(new Dimension(360, 520));

        // Circular brand avatar (stands in for a Google-style account icon)
        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.PRIMARY_BLUE);
                g2.fill(new Ellipse2D.Double(0, 0, getWidth(), getHeight()));
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 30));
                FontMetrics fm = g2.getFontMetrics();
                String letter = "J";
                int tx = (getWidth() - fm.stringWidth(letter)) / 2;
                int ty = (getHeight() + fm.getAscent()) / 2 - 4;
                g2.drawString(letter, tx, ty);
            }
        };
        avatar.setPreferredSize(new Dimension(64, 64));
        avatar.setMaximumSize(new Dimension(64, 64));
        avatar.setOpaque(false);
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Sign in");
        title.setFont(new Font("SansSerif", Font.PLAIN, 26));
        title.setForeground(UITheme.TEXT_DARK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("to continue to Miller Library System");
        subtitle.setFont(UITheme.FONT_SMALL);
        subtitle.setForeground(UITheme.TEXT_GRAY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        emailField = UITheme.styledTextField();
        passwordField = UITheme.styledPasswordField();

        card.add(avatar);
        card.add(Box.createVerticalStrut(16));
        card.add(title);
        card.add(Box.createVerticalStrut(4));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(28));
        card.add(fieldBlock("Email", emailField));
        card.add(Box.createVerticalStrut(16));
        card.add(fieldBlock("Password", passwordField));
        card.add(Box.createVerticalStrut(30));

        JPanel actionsRow = new JPanel(new BorderLayout());
        actionsRow.setOpaque(false);
        actionsRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        actionsRow.setMaximumSize(new Dimension(280, 45));

        JButton createAccountBtn = new JButton("Create account");
        createAccountBtn.setForeground(UITheme.ACCENT_BLUE);
        createAccountBtn.setFont(UITheme.FONT_BODY);
        createAccountBtn.setBorderPainted(false);
        createAccountBtn.setContentAreaFilled(false);
        createAccountBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        createAccountBtn.addActionListener(e -> new RegisterDialog(this).setVisible(true));

        JButton signInBtn = UITheme.primaryButton("Sign In");
        signInBtn.addActionListener(e -> attemptLogin());

        actionsRow.add(createAccountBtn, BorderLayout.WEST);
        actionsRow.add(signInBtn, BorderLayout.EAST);

        card.add(actionsRow);

        // Wrapper keeps the card centered regardless of frame size
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.add(card);
        return wrapper;
    }

    private JPanel fieldBlock(String label, JComponent field) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(Component.CENTER_ALIGNMENT);
        block.setMaximumSize(new Dimension(280, 60));

        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.FONT_SMALL);
        lbl.setForeground(UITheme.TEXT_GRAY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(280, 38));

        block.add(lbl);
        block.add(Box.createVerticalStrut(4));
        block.add(field);
        return block;
    }

    private void attemptLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both email and password.",
                    "Missing information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Document user = userService.authenticate(email, password);
        if (user == null) {
            JOptionPane.showMessageDialog(this, "Incorrect email or password. Please try again.",
                    "Sign in failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this, "Welcome back, " + user.getString("name") + "!");
        new MainFrame(user).setVisible(true);
        this.dispose();
    }
}
