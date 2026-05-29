package server.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.*;
import server.MultiChatServer;

public class ServerView extends JFrame implements ServerListener {
    private JTextField portField;
    private JButton startStopBtn;
    private JTextArea logArea;
    private JList<String> clientList;
    private DefaultListModel<String> clientListModel;
    private JLabel statusLabel;

    private MultiChatServer server;
    private Thread serverThread;

    public ServerView() {
        setTitle("Multi-Client Chat Server Manager");
        setSize(700, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // North: Control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        controlPanel.add(new JLabel("Port:"));
        portField = new JTextField("5000", 6);
        controlPanel.add(portField);

        startStopBtn = new JButton("Start Server");
        startStopBtn.setPreferredSize(new Dimension(120, 26));
        controlPanel.add(startStopBtn);

        // Database indicator
        JLabel dbLabel = new JLabel("Database: PostgreSQL (chatapp)");
        controlPanel.add(dbLabel);

        add(controlPanel, BorderLayout.NORTH);

        // Center: JSplitPane splitting Logs (Left) and Clients (Right)
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("System Logs"));

        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        JScrollPane clientScrollPane = new JScrollPane(clientList);
        clientScrollPane.setPreferredSize(new Dimension(220, 0));
        clientScrollPane.setBorder(BorderFactory.createTitledBorder("Connected Clients"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, logScrollPane, clientScrollPane);
        splitPane.setResizeWeight(0.7);
        add(splitPane, BorderLayout.CENTER);

        // South: Footer
        statusLabel = new JLabel("Status: Stopped");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        add(statusLabel, BorderLayout.SOUTH);

        // Action Listener for toggle button
        startStopBtn.addActionListener(e -> {
            if (server == null || !server.isRunning()) {
                startServer();
            } else {
                stopServer();
            }
        });
    }

    private void startServer() {
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Port Number", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        server = new MultiChatServer(port);
        server.setListener(this);

        serverThread = new Thread(() -> {
            server.startServer();
        });
        serverThread.start();
    }

    private void stopServer() {
        if (server != null) {
            server.stopServer();
            server = null;
        }
        if (serverThread != null) {
            serverThread.interrupt();
            serverThread = null;
        }
    }

    // ServerListener implementation
    @Override
    public void onServerStarted() {
        SwingUtilities.invokeLater(() -> {
            startStopBtn.setText("Stop Server");
            portField.setEnabled(false);
            statusLabel.setText("Status: Running on Port " + portField.getText().trim() + " (Clients: 0)");
        });
    }

    @Override
    public void onServerStopped() {
        SwingUtilities.invokeLater(() -> {
            startStopBtn.setText("Start Server");
            portField.setEnabled(true);
            clientListModel.clear();
            statusLabel.setText("Status: Stopped");
        });
    }

    @Override
    public void onClientConnected(String username, String displayName, String ipAddress) {
        SwingUtilities.invokeLater(() -> {
            String clientInfo = displayName + " (" + username + ") [" + ipAddress + "]";
            clientListModel.addElement(clientInfo);
            updateStatusLabel();
        });
    }

    @Override
    public void onClientDisconnected(String username) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < clientListModel.size(); i++) {
                String item = clientListModel.get(i);
                if (item.contains("(" + username + ")")) {
                    clientListModel.remove(i);
                    break;
                }
            }
            updateStatusLabel();
        });
    }

    private void updateStatusLabel() {
        int clients = clientListModel.size();
        statusLabel.setText("Status: Running on Port " + portField.getText().trim() + " (Clients: " + clients + ")");
    }

    @Override
    public void onLogMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            // Auto scroll to bottom
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}
