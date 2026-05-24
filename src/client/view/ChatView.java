package client.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import client.controller.ChatController;

public class ChatView extends JFrame {
    private ChatController controller;

    private JTabbedPane tabbedPane;
    private Map<Integer, JTextPane> groupChatAreas = new HashMap<>(); // groupId -> JTextPane
    private List<Integer> groupTabIds = new ArrayList<>();

    private JTextPane publicChatArea; // tab 0 - always presnet
    private JTextField inputField;

    private DefaultListModel<String> userModel;
    private JList<String> onlineUserlist;

    private DefaultListModel<String> groupModel;
    private JList<String> groupList;

    public ChatView(ChatController controller) {
        this.controller = controller;
        setTitle("Chat Room");
        setSize(750, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        tabbedPane = new JTabbedPane();
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        // chat area (showing message) (uneditable)
        publicChatArea = new JTextPane();
        publicChatArea.setContentType("text/html");
        publicChatArea.setEditable(false);

        // listening Hyberlink-related Event inside publicChatArea Component
        publicChatArea.addHyperlinkListener(e -> {
            if (e.getEventType() == EventType.ACTIVATED) {
                String href = e.getDescription(); // get href value in <a> tag
                if (href.startsWith("del:")) {

                    // delete request
                    int messageId = Integer.parseInt(href.split(":")[1]);
                    controller.deleteMessage(messageId, "ALL");
                } else {
                    controller.requestFileDownload(href);
                }

            }
        });

        tabbedPane.addTab("Public Chat", new JScrollPane(publicChatArea));
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // user's group area
        JPanel userGroupsPanel = new JPanel(new BorderLayout(10, 10));
        userGroupsPanel.add(new JLabel("My Groups"), BorderLayout.NORTH);

        groupModel = new DefaultListModel<>();
        groupList = new JList<>(groupModel);

        JScrollPane groupListScrollPane = new JScrollPane(groupList);
        userGroupsPanel.add(groupListScrollPane, BorderLayout.CENTER);

        JPanel userGroupCTAPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton newGroupBtn = new JButton("+ New");
        newGroupBtn.setPreferredSize(new Dimension(100, 28));
        JButton joinGroupBtn = new JButton("Join");
        joinGroupBtn.setPreferredSize(new Dimension(100, 28));

        userGroupCTAPanel.add(newGroupBtn);
        userGroupCTAPanel.add(joinGroupBtn);

        userGroupsPanel.add(userGroupCTAPanel, BorderLayout.SOUTH);

        mainPanel.add(userGroupsPanel, BorderLayout.WEST);

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
        inputPanel.add(inputField, BorderLayout.CENTER);

        JPanel sendBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton sendBtn = new JButton("Send");
        JButton sendFileBtn = new JButton("📎");
        sendFileBtn.setToolTipText("Send a file");

        sendBtnPanel.add(sendBtn);
        sendBtnPanel.add(sendFileBtn);
        inputPanel.add(sendBtnPanel, BorderLayout.EAST);

        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        // quit area
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearHistoryBtn = new JButton("Clear History");
        JButton quitBtn = new JButton("QUIT");
        topPanel.add(clearHistoryBtn);
        topPanel.add(quitBtn);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        this.add(mainPanel);

        // not bring these operations into a new thread because they almost take instant
        // time

        // click 'Send' btn
        sendBtn.addActionListener(e -> {
            String text = inputField.getText();
            if (text.isEmpty())
                return;

            // read the active tab to decide routing
            int selectedTab = tabbedPane.getSelectedIndex();
            if (selectedTab == 0) { // Public Chat tab
                this.controller.sendMessages(text);
            } else {
                int groupId = groupTabIds.get(selectedTab - 1);

                controller.sendGroupMessage(groupId, text);
            }

            inputField.setText("");
            inputField.requestFocus();
        });

        // click "🔗" to send file
        sendFileBtn.addActionListener(e -> {
            int selectedTab = tabbedPane.getSelectedIndex();
            String target;

            if (selectedTab == 0)
                target = "ALL"; // public chat
            else {
                int groupId = groupTabIds.get(selectedTab - 1);
                target = String.valueOf(groupId);
            }

            controller.sendFile(target);
        });

        // press 'Enter' in input field
        inputField.addActionListener(e -> {
            String text = inputField.getText();
            if (text.isEmpty())
                return;

            // read the active tab to decide routing
            int selectedTab = tabbedPane.getSelectedIndex();
            if (selectedTab == 0) { // Public Chat tab
                this.controller.sendMessages(text);
            } else {
                int groupId = groupTabIds.get(selectedTab - 1);

                controller.sendGroupMessage(groupId, text);
            }

            inputField.setText("");
            inputField.requestFocus();
        });

        // Quit Button
        quitBtn.addActionListener(e -> {
            this.controller.disconnect();
        });

        clearHistoryBtn.addActionListener(e -> {
            int selectedTab = tabbedPane.getSelectedIndex();
            String target = (selectedTab == 0)
                    ? "ALL"
                    : String.valueOf(groupTabIds.get(selectedTab - 1));
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "This will permanently delete all YOUR messages in this conversation.\nThis action cannot be undone.",
                    "Clear History",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                controller.clearHistory(target);
            }
        });

        // + New btn in My Groups section
        newGroupBtn.addActionListener(e -> {
            new CreateGroupDialog(this, controller).setVisible(true);
        });

        // Join btn in My Groups section
        joinGroupBtn.addActionListener(e -> {
            new JoinGroupDialog(this, controller).setVisible(true);
        });

        // Leave group - right click to show popup 'Leave Group'
        JPopupMenu groupPopup = new JPopupMenu();
        JMenuItem leaveItem = new JMenuItem("Leave Group");
        groupPopup.add(leaveItem);

        groupList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    showPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    showPopup(e);
            }

            private void showPopup(MouseEvent e) {
                int idx = groupList.locationToIndex(e.getPoint());
                if (idx >= 0) {
                    groupList.setSelectedIndex(idx);
                    groupPopup.show(groupList, e.getX(), e.getY());
                }
            }
        });

        leaveItem.addActionListener(e -> {
            String selected = groupList.getSelectedValue();

            if (selected == null)
                return;

            // regex
            int groupID = Integer.parseInt(selected.replaceAll(".*\\((\\d+)\\)", "$1"));
            controller.leaveGroup(groupID);
        });
    }

    public void updateMessageDeleted(int messageId, String sender) {
        SwingUtilities.invokeLater(() -> {
            replaceMessageHTML(publicChatArea, messageId, sender);

            for (JTextPane area : groupChatAreas.values()) {
                replaceMessageHTML(area, messageId, sender);
            }
        });
    }

    public void clearChatArea(String target) {
        SwingUtilities.invokeLater(() -> {
            JTextPane pane;
            if (target.equals("ALL")) {
                pane = publicChatArea;
            } else {
                int groupId = Integer.parseInt(target);
                pane = groupChatAreas.get(groupId);
            }
            if (pane != null) {
                pane.setText("");
            }
        });
    }

    private void replaceMessageHTML(JTextPane pane, int messageId, String sender) {
        try {
            HTMLDocument doc = (HTMLDocument) pane.getDocument();
            Element elem = doc.getElement("msg-" + messageId);

            if (elem != null) {
                String replacement = String.format("<div><b>[%s]</b>: <i>[This message was deleted]</i></div>", sender);

                doc.setOuterHTML(elem, replacement);
            }
        } catch (Exception e) {
            System.err.println("Failed to replace message HTML: " + e.getMessage());
        }
    }

    // append function for append HTML file
    public void appendToPane(JTextPane pane, String msg) {
        SwingUtilities.invokeLater(() -> {
            try {
                HTMLDocument doc = (HTMLDocument) pane.getDocument();
                HTMLEditorKit kit = (HTMLEditorKit) pane.getEditorKit();

                // // only accpet <a> tag
                // if (msg.contains("<a href="))
                // safeMsg = msg;

                kit.insertHTML(doc, doc.getLength(), "<div>" + msg + "</div>", 0, 0, null);
                pane.setCaretPosition(doc.getLength());

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // controller call this func when having new messages from background thread
    // (ReadThread)
    public void displayMessage(String text) {
        // update UI in EDT thread
        SwingUtilities.invokeLater(() -> {
            appendToPane(publicChatArea, text);

            // scroll to the last line
            publicChatArea.setCaretPosition(publicChatArea.getDocument().getLength());
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

    public void displayGroupMessage(int groupId, String text) {
        SwingUtilities.invokeLater(() -> {
            JTextPane area = groupChatAreas.get(groupId);

            if (area == null)
                return;

            // area.append(text + "\n");
            appendToPane(area, text);
            area.setCaretPosition(area.getDocument().getLength());
        });
    }

    // groupList = "study:3,game:4"
    public void updateGroupList(String groupList) {
        SwingUtilities.invokeLater(() -> {
            // revmoe all group tabs, keep tab 0 (Public CHat)
            while (tabbedPane.getTabCount() > 1) {
                tabbedPane.remove(1);
            }
            groupChatAreas.clear();
            groupTabIds.clear();

            groupModel.clear();

            if (groupList == null || groupList.isEmpty())
                return;

            String[] groupListParts = groupList.split(",");
            for (String entry : groupListParts) {
                String[] parts = entry.split(":");
                String name = parts[0];
                int id = Integer.parseInt(parts[1]);

                // create new tab
                JTextPane area = new JTextPane();
                area.setEditable(false);
                area.setContentType("text/html");
                area.setEditable(false);

                final int currentGroupId = id;
                area.addHyperlinkListener(e -> {
                    if (e.getEventType() == EventType.ACTIVATED) {
                        String href = e.getDescription(); // get href value in <a> tag

                        if (href.startsWith("del:")) {
                            // format: "del:msgId"
                            int messageId = Integer.parseInt(href.split(":")[1]);
                            controller.deleteMessage(messageId, String.valueOf(currentGroupId)); // group target
                        } else {
                            controller.requestFileDownload(href);
                        }

                    }
                });
                tabbedPane.addTab(name, new JScrollPane(area));
                groupChatAreas.put(id, area);
                groupTabIds.add(id);

                controller.loadHistory(String.valueOf(id), 0); // load history for this group

                // update WEST list
                groupModel.addElement("📂 " + name + " (" + id + ")");
            }
        });
    }

    public void promptFileSave(String filename, byte[] fileData) {
        // open file chooser
        JFileChooser saveChooser = new JFileChooser();
        saveChooser.setDialogTitle("Save file as...");
        saveChooser.setSelectedFile(new File(filename)); // pre-fill the filename

        int saveResult = saveChooser.showSaveDialog(this);
        if (saveResult != JFileChooser.APPROVE_OPTION)
            return;

        File saveLocation = saveChooser.getSelectedFile();

        // write byte array to disk in background thread
        new Thread(() -> {
            try {
                Files.write(saveLocation.toPath(), fileData);

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "File saved to:\n" + saveLocation.getAbsolutePath(),
                            "Save Successful", JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Failed to save file: " + ex.getMessage(),
                        "Save Error", JOptionPane.ERROR_MESSAGE));
            }

        }).start();

    }
}
