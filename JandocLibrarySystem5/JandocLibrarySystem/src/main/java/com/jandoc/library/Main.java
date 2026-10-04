package com.jandoc.library;

import com.jandoc.library.ui.LoginFrame;

import javax.swing.*;

/**
 * Entry point for the Jandoc Library Management System.
 * Launches the Google-style sign-in screen; MainFrame opens after
 * a successful login.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // fall back to default look and feel
            }
            new LoginFrame();
        });
    }
}
