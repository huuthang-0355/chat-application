package client.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import client.controller.ChatController;

public class RegisterView extends JFrame {

    private ChatController controller;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField hostField;
    private JTextField portField;
    private JLabel statusLabel;

    public RegisterView(LoginView loginView) {
        controller = new ChatController();

        setTitle("Chatting Register");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // align center

        // main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // heading
        JPanel headingPanel = new JPanel();
        JLabel heading = new JLabel("Register to Connect!");
        heading.setFont(new Font("Arial", Font.BOLD, 20));
        headingPanel.add(heading);

        mainPanel.add(headingPanel, BorderLayout.NORTH);

        // Form panel using GridBagLayout
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5); // padding

        // Username row
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.4;
        formPanel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        usernameField = new JTextField("thangthanthien", 20);
        formPanel.add(usernameField, gbc);

        // Password row
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.4;
        formPanel.add(new JLabel("Password: "), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        passwordField = new JPasswordField(20);
        formPanel.add(passwordField, gbc);

        // Host row
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.4;
        formPanel.add(new JLabel("Host:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        hostField = new JTextField("localhost", 20);
        formPanel.add(hostField, gbc);

        // Port row
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.4;
        formPanel.add(new JLabel("Port:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        portField = new JTextField("5000", 20);
        formPanel.add(portField, gbc);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton connectBtn = new JButton("Connect");
        connectBtn.setPreferredSize(new Dimension(120, 35));

        JButton backBtn = new JButton("Back");
        backBtn.setPreferredSize(new Dimension(120, 35));

        buttonPanel.add(connectBtn);
        buttonPanel.add(backBtn);

        // Status label
        statusLabel = new JLabel("Not Connected", SwingConstants.CENTER);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Add to main panel
        mainPanel.add(formPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(buttonPanel, BorderLayout.NORTH);
        bottomPanel.add(statusLabel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        connectBtn.addActionListener(e -> {
            statusLabel.setText("Connecting...");
            connectBtn.setEnabled(false);

            String user = usernameField.getText().trim();
            char[] passChars = passwordField.getPassword();
            String password = new String(passChars);
            Arrays.fill(passChars, '\0');

            String host = hostField.getText().trim();
            int port = Integer.parseInt(portField.getText());

            // socket connection is a BLOCKING operation --> bring it to seperate thread.
            // if not, the UI will be 'Treo' during waiting for connection period.
            new Thread(() -> {
                controller.connect(user, password, true, host, port, loginView);
            }).start();

        });

        backBtn.addActionListener(e -> {
            // close register view --> show login view
            SwingUtilities.invokeLater(() -> {
                this.dispose();

                loginView.showGUI();
            });
        });
    }

    public void showGUI() {
        this.setVisible(true);
    }

    public void setStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
        });
    }
}
