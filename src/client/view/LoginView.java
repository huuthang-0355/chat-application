package client.view;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import client.controller.ChatController;

import java.awt.*;
import java.util.Arrays;

public class LoginView extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField hostField;
    private JTextField portField;
    private JTextField displayNameField;
    private JLabel displayNameLabel;
    private JLabel statusLabel;
    private JRadioButton registerRadioBtn;
    private JRadioButton loginRadioBtn;
    private ChatController controller;

    public LoginView() {

        controller = new ChatController();

        setTitle("Chatting Login");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // align center

        // main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Radio: Login & Register
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        loginRadioBtn = new JRadioButton("LOGIN");
        loginRadioBtn.setSelected(true);

        registerRadioBtn = new JRadioButton("REGISTER");

        ButtonGroup group = new ButtonGroup();
        group.add(loginRadioBtn);
        group.add(registerRadioBtn);

        radioPanel.add(loginRadioBtn);
        radioPanel.add(registerRadioBtn);

        mainPanel.add(radioPanel, BorderLayout.NORTH);

        // Form panel using GridBagLayout
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5); // padding

        // Display Name row (initially hidden, placed at the top)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.4;
        displayNameLabel = new JLabel("Display Name:");
        displayNameLabel.setVisible(false);
        formPanel.add(displayNameLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        displayNameField = new JTextField(20);
        displayNameField.setVisible(false);
        formPanel.add(displayNameField, gbc);

        // Username row
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.4;
        formPanel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        usernameField = new JTextField(20);
        formPanel.add(usernameField, gbc);

        // Password row
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.4;
        formPanel.add(new JLabel("Password: "), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        passwordField = new JPasswordField(20);
        formPanel.add(passwordField, gbc);

        // Host row
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.4;
        formPanel.add(new JLabel("Host:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        hostField = new JTextField("localhost", 20);
        formPanel.add(hostField, gbc);

        // Port row
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.4;
        formPanel.add(new JLabel("Port:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        portField = new JTextField("5000", 20);
        formPanel.add(portField, gbc);

        // Radio button listeners to dynamically show/hide the display name row
        loginRadioBtn.addActionListener(e -> {
            displayNameLabel.setVisible(false);
            displayNameField.setVisible(false);
            this.revalidate();
            this.repaint();
        });

        registerRadioBtn.addActionListener(e -> {
            displayNameLabel.setVisible(true);
            displayNameField.setVisible(true);
            this.revalidate();
            this.repaint();
        });

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton connectBtn = new JButton("Connect");
        connectBtn.setPreferredSize(new Dimension(120, 35));

        buttonPanel.add(connectBtn);

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

            String user = usernameField.getText().trim();
            char[] passChars = passwordField.getPassword();
            String password = new String(passChars);
            Arrays.fill(passChars, '\0');

            boolean isRegister = registerRadioBtn.isSelected();
            String displayName = displayNameField.getText().trim();

            if (isRegister && displayName.isEmpty()) {
                statusLabel.setText("Display name cannot be empty");
                return;
            }

            String host = hostField.getText().trim();
            int port = Integer.parseInt(portField.getText());

            // socket connection is a BLOCKING operation --> bring it to seperate thread.
            // if not, the UI will be 'Treo' during waiting for connection period.
            new Thread(() -> {
                controller.connect(user, password, displayName, isRegister, host, port, this);
            }).start();

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
