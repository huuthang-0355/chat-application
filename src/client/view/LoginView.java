package client.view;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import client.controller.ChatController;

import java.awt.*;

public class LoginView extends JFrame {
    private JTextField usernameField;
    private JTextField hostField;
    private JTextField portField;
    private JLabel statusLabel;
    private ChatController controller;

    public LoginView() {

        controller = new ChatController();

        setTitle("Chatting Login");
        setSize(400, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // align center

        // main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // heading
        JPanel headingPanel = new JPanel();
        JLabel heading = new JLabel("Login to Connect!");
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

        // Host row
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.4;
        formPanel.add(new JLabel("Host:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        hostField = new JTextField("localhost", 20);
        formPanel.add(hostField, gbc);

        // Port row
        gbc.gridx = 0;
        gbc.gridy = 2;
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
            connectBtn.setEnabled(false);

            String user = usernameField.getText().trim();
            String host = hostField.getText().trim();
            int port = Integer.parseInt(portField.getText());

            // socket connection is a BLOCKING operation --> bring it to seperate thread.
            // if not, the UI will be 'Treo' during waiting for connection period.
            new Thread(() -> {
                controller.connect(user, host, port, this);
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
