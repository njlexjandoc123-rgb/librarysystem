package com.jandoc.library.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import org.bson.Document;

/**
 * Main application window shown after a successful sign-in.
 * Hosts the Books, Members, and Borrow/Return tabs.
 */
public class MainFrame extends JFrame {

    public MainFrame(Document currentUser) {
        setTitle("Miller Library Management System");
        setSize(1000, 650);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(UITheme.BACKGROUND);
        setLayout(new BorderLayout());

        add(buildHeader(currentUser), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UITheme.FONT_HEADING.deriveFont(14f));
        tabs.setBackground(UITheme.WHITE);
        tabs.addTab("  Books  ", new BookPanel());
        tabs.addTab("  Members  ", new UserPanel());
        tabs.addTab("  Borrow / Return  ", new BorrowPanel());

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildHeader(Document currentUser) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.PRIMARY_BLUE);
        header.setBorder(new EmptyBorder(14, 24, 14, 24));

        JLabel titleLabel = new JLabel("Miller Library Management System");
        titleLabel.setFont(UITheme.FONT_TITLE.deriveFont(20f));
        titleLabel.setForeground(UITheme.WHITE);

        String name = currentUser != null ? currentUser.getString("name") : "Guest";
        String role = currentUser != null ? currentUser.getString("role") : "";
        JLabel userLabel = new JLabel(name + "  \u2022  " + role + "  ");
        userLabel.setFont(UITheme.FONT_BODY);
        userLabel.setForeground(UITheme.LIGHT_BLUE);

        header.add(titleLabel, BorderLayout.WEST);
        header.add(userLabel, BorderLayout.EAST);
        return header;
    }
}
