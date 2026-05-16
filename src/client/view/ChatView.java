package client.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.PageAttributes;
import java.awt.Panel;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import client.controller.ChatController;

public class ChatView extends JFrame {
    private ChatController controller;
    private JTextArea chatArea;
    private JTextField inputField;

    private DefaultListModel<String> userModel;
    private JList<String> onlineUserlist;

    public ChatView(ChatController controller) {
        this.controller = controller;
        setTitle("Chat Room");
        setSize(750, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // chat area (showing message) (uneditable)
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        // add ScrollPane
        mainPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // online users area
        JPanel onlineUsersPanel = new JPanel(new BorderLayout(10, 10));
        onlineUsersPanel.add(new JLabel("Online Users"), BorderLayout.NORTH);

        userModel = new DefaultListModel<>();
        onlineUserlist = new JList<>(userModel);

        onlineUserlist.setForeground(Color.GREEN);

        JScrollPane scrollPane = new JScrollPane(onlineUserlist);
        onlineUsersPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(onlineUsersPanel, BorderLayout.EAST);

        // input area
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        JButton sendBtn = new JButton("Send");
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        // quit area
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton quitBtn = new JButton("QUIT");
        topPanel.add(quitBtn);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        this.add(mainPanel);

        // not bring these operations into a new thread because they almost take instant
        // time.

        // click 'Send' btn
        sendBtn.addActionListener(e -> {
            String text = inputField.getText();
            controller.sendMessages(text);
            inputField.setText("");
            inputField.requestFocus();
        });

        // press 'Enter' in input field
        inputField.addActionListener(e -> {
            String text = inputField.getText();
            controller.sendMessages(text);
            inputField.setText("");
            inputField.requestFocus();
        });

        // Quit Button
        quitBtn.addActionListener(e -> {
            controller.disconnect();
        });
    }

    // controller call this func when having new messages from background thread
    // (ReadThread)
    public void displayMessage(String text) {
        // update UI in EDT thread
        SwingUtilities.invokeLater(() -> {
            chatArea.append(text + "\n");

            // scroll to the last line
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    public void updateUserList(String[] onlineUserList) {
        SwingUtilities.invokeLater(() -> {
            userModel.clear();

            for (String onlineUser : onlineUserList) {
                userModel.addElement(onlineUser);
            }
        });
    }
}
