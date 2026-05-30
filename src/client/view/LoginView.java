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
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;

import client.controller.ChatController;

import java.awt.*;
import java.util.Arrays;

public class LoginView extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField hostField;
    private JTextField portField;
    private JTextField displayNameField;
    private JComboBox<client.config.ServerConfigManager.ServerItem> serverCombo;
    private JLabel displayNameLabel;
    private JLabel statusLabel;
    private JRadioButton registerRadioBtn;
    private JRadioButton loginRadioBtn;
    private ChatController controller;

    public LoginView() {

        controller = new ChatController();

        setTitle("Chatting Login");
        setSize(800, 520);
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

        // Server Select Combo row
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.4;
        formPanel.add(new JLabel("Server Bookmark:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        java.util.List<client.config.ServerConfigManager.ServerItem> savedServers = client.config.ServerConfigManager.loadServers();
        DefaultComboBoxModel<client.config.ServerConfigManager.ServerItem> comboModel = new DefaultComboBoxModel<>();
        for (client.config.ServerConfigManager.ServerItem item : savedServers) {
            comboModel.addElement(item);
        }
        serverCombo = new JComboBox<>(comboModel);
        formPanel.add(serverCombo, gbc);

        // Host row
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.4;
        formPanel.add(new JLabel("Host:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        hostField = new JTextField("localhost", 20);
        formPanel.add(hostField, gbc);

        // Port row
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0.4;
        formPanel.add(new JLabel("Port:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        portField = new JTextField("5000", 20);
        formPanel.add(portField, gbc);

        // Server list buttons row
        JPanel serverButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        JButton addServerBtn = new JButton("Add Bookmark");
        JButton updateServerBtn = new JButton("Update");
        JButton deleteServerBtn = new JButton("Delete");
        serverButtonsPanel.add(addServerBtn);
        serverButtonsPanel.add(updateServerBtn);
        serverButtonsPanel.add(deleteServerBtn);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        formPanel.add(serverButtonsPanel, gbc);
        gbc.gridwidth = 1; // reset

        // Wire Combobox selection loader
        serverCombo.addActionListener(e -> {
            client.config.ServerConfigManager.ServerItem selected = (client.config.ServerConfigManager.ServerItem) serverCombo.getSelectedItem();
            if (selected != null) {
                hostField.setText(selected.host);
                portField.setText(String.valueOf(selected.port));
            }
        });

        // Initialize connection text boxes with first saved server details
        if (serverCombo.getItemCount() > 0) {
            serverCombo.setSelectedIndex(0);
            client.config.ServerConfigManager.ServerItem selected = serverCombo.getItemAt(0);
            hostField.setText(selected.host);
            portField.setText(String.valueOf(selected.port));
        }

        // Add Bookmark logic
        addServerBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Enter Server Bookmark Name:", "Add Server Connection", JOptionPane.QUESTION_MESSAGE);
            if (name != null && !name.trim().isEmpty()) {
                String host = hostField.getText().trim();
                int port = 5000;
                try {
                    port = Integer.parseInt(portField.getText().trim());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid Port number", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                client.config.ServerConfigManager.ServerItem newItem = new client.config.ServerConfigManager.ServerItem(name.trim(), host, port);
                comboModel.addElement(newItem);
                serverCombo.setSelectedItem(newItem);
                
                // save list
                java.util.List<client.config.ServerConfigManager.ServerItem> list = new java.util.ArrayList<>();
                for (int i = 0; i < comboModel.getSize(); i++) {
                    list.add(comboModel.getElementAt(i));
                }
                client.config.ServerConfigManager.saveServers(list);
            }
        });

        // Update selected bookmark
        updateServerBtn.addActionListener(e -> {
            client.config.ServerConfigManager.ServerItem selected = (client.config.ServerConfigManager.ServerItem) serverCombo.getSelectedItem();
            if (selected != null) {
                String host = hostField.getText().trim();
                int port = 5000;
                try {
                    port = Integer.parseInt(portField.getText().trim());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid Port number", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                selected.host = host;
                selected.port = port;
                serverCombo.repaint();
                
                // save list
                java.util.List<client.config.ServerConfigManager.ServerItem> list = new java.util.ArrayList<>();
                for (int i = 0; i < comboModel.getSize(); i++) {
                    list.add(comboModel.getElementAt(i));
                }
                client.config.ServerConfigManager.saveServers(list);
                JOptionPane.showMessageDialog(this, "Server configuration updated!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // Delete selected bookmark
        deleteServerBtn.addActionListener(e -> {
            client.config.ServerConfigManager.ServerItem selected = (client.config.ServerConfigManager.ServerItem) serverCombo.getSelectedItem();
            if (selected != null) {
                if (comboModel.getSize() <= 1) {
                    JOptionPane.showMessageDialog(this, "Cannot delete the last server bookmark.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                comboModel.removeElement(selected);
                
                // save list
                java.util.List<client.config.ServerConfigManager.ServerItem> list = new java.util.ArrayList<>();
                for (int i = 0; i < comboModel.getSize(); i++) {
                    list.add(comboModel.getElementAt(i));
                }
                client.config.ServerConfigManager.saveServers(list);
                
                // load first element
                serverCombo.setSelectedIndex(0);
            }
        });

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
