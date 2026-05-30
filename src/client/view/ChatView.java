package client.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
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
    private Map<String, JTextPane> chatTabsMap = new HashMap<>(); // targetId -> JTextPane
    private Map<String, String> userDisplayNameMap = new java.util.HashMap<>();
    private Map<String, Boolean> historyLoadedMap = new HashMap<>();
    private Map<String, List<String>> pendingMessagesMap = new HashMap<>();

    private JTextPane publicChatArea; // tab 0 - always presnet
    private JTextArea inputField;
    private JCheckBox enterToSendCheckbox;

    private DefaultListModel<String> userModel;
    private JList<String> onlineUserlist;

    private DefaultListModel<String> groupModel;
    private JList<String> groupList;

    private JPanel statusPanel;
    private JLabel statusLabel;
    private JProgressBar statusProgressBar;

    public ChatView(ChatController controller) {
        this.controller = controller;
        setTitle("Chat Room");
        setSize(1100, 720);
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

        groupList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = groupList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        String entry = groupModel.getElementAt(index);
                        int idStart = entry.lastIndexOf('(');
                        if (idStart != -1) {
                            String idStr = entry.substring(idStart + 1, entry.length() - 1);
                            ensureTabOpen(idStr);
                        }
                    }
                }
            }
        });

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

        onlineUserlist.setCellRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                String entry = value.toString();
                if (entry.contains(":")) {
                    String[] parts = entry.split(":", 2);
                    label.setText("● " + parts[0]);
                } else {
                    label.setText(entry);
                }
                return label;
            }
        });

        onlineUserlist.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = onlineUserlist.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        String entry = userModel.getElementAt(index);
                        if (entry.contains(":")) {
                            String[] parts = entry.split(":", 2);
                            String username = parts[1];
                            if (!username.equals(controller.getUsername())) {
                                ensureTabOpen(username);
                            }
                        } else {
                            if (!entry.equals(controller.getUsername())) {
                                ensureTabOpen(entry);
                            }
                        }
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(onlineUserlist);
        onlineUsersPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(onlineUsersPanel, BorderLayout.EAST);

        // input area
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextArea(2, 30);
        inputField.setLineWrap(true);
        inputField.setWrapStyleWord(true);
        JScrollPane inputScrollPane = new JScrollPane(inputField);
        inputScrollPane.setPreferredSize(new Dimension(0, 45));
        inputPanel.add(inputScrollPane, BorderLayout.CENTER);

        JPanel sendBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton emojiBtn = new JButton("😊");
        emojiBtn.setToolTipText("Insert an emoji");
        enterToSendCheckbox = new JCheckBox("Enter to Send", true);
        JButton sendBtn = new JButton("Send");
        JButton sendFileBtn = new JButton("📎");
        sendFileBtn.setToolTipText("Send a file");

        // Create Emoji popup menu in a grid layout for rich aesthetics
        JPopupMenu emojiMenu = new JPopupMenu();
        emojiMenu.setLayout(new java.awt.GridLayout(2, 5));
        String[] emojis = { "😊", "😂", "😍", "😢", "👍", "😮", "💖", "👏", "🎉", "🔥" };
        for (String emoji : emojis) {
            JButton btn = new JButton(emoji);
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            btn.setMargin(new java.awt.Insets(0, 0, 0, 0));
            btn.setBorder(BorderFactory.createEmptyBorder());
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
            btn.setPreferredSize(new Dimension(32, 32));
            btn.addActionListener(ev -> {
                inputField.insert(emoji, inputField.getCaretPosition());
                inputField.requestFocus();
                emojiMenu.setVisible(false);
            });
            emojiMenu.add(btn);
        }
        emojiBtn.addActionListener(e -> {
            emojiMenu.show(emojiBtn, 0, -emojiMenu.getPreferredSize().height);
        });

        sendBtnPanel.add(emojiBtn);
        sendBtnPanel.add(enterToSendCheckbox);
        sendBtnPanel.add(sendBtn);
        sendBtnPanel.add(sendFileBtn);
        inputPanel.add(sendBtnPanel, BorderLayout.EAST);

        // container for south panel (input + status progress)
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(inputPanel, BorderLayout.NORTH);

        // Status Panel for file transfer progress
        statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusPanel.setBackground(new Color(245, 245, 245));
        statusLabel = new JLabel(" ");
        statusProgressBar = new JProgressBar();
        statusProgressBar.setIndeterminate(true);
        statusProgressBar.setPreferredSize(new Dimension(150, 14));

        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.add(statusProgressBar, BorderLayout.EAST);
        statusPanel.setVisible(false); // Hidden by default

        southPanel.add(statusPanel, BorderLayout.SOUTH);
        mainPanel.add(southPanel, BorderLayout.SOUTH);

        // quit area
        JPanel topPanel = new JPanel(new BorderLayout());

        // Left side: Logged-in Username display
        JLabel usernameLabel = new JLabel(controller.getDisplayName() + " ("
                + controller.getUsername() + ") | Server: " + controller.getHost() + ":" + controller.getPort());
        usernameLabel.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        usernameLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        topPanel.add(usernameLabel, BorderLayout.WEST);

        // Right side: Controls
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearHistoryBtn = new JButton("Clear History");
        JButton quitBtn = new JButton("QUIT");
        btnPanel.add(clearHistoryBtn);
        btnPanel.add(quitBtn);
        topPanel.add(btnPanel, BorderLayout.EAST);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        this.add(mainPanel);

        // not bring these operations into a new thread because they almost take instant
        // time

        // click 'Send' btn
        sendBtn.addActionListener(e -> {
            sendMessageAction();
        });

        // click "🔗" to send file
        sendFileBtn.addActionListener(e -> {
            int selectedTab = tabbedPane.getSelectedIndex();
            String target;

            if (selectedTab == 0)
                target = "ALL"; // public chat
            else {
                java.awt.Component comp = tabbedPane.getTabComponentAt(selectedTab);
                if (comp instanceof ClosableTabComponent) {
                    target = ((ClosableTabComponent) comp).getTargetId();
                } else {
                    return;
                }
            }

            controller.sendFile(target);
        });

        // Intercept enter key in JTextArea
        inputField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    if (enterToSendCheckbox.isSelected()) {
                        if (!e.isShiftDown()) {
                            e.consume(); // prevent inserting a newline
                            sendMessageAction();
                        }
                    }
                }
            }
        });

        // Quit Button
        quitBtn.addActionListener(e -> {
            this.controller.disconnect();
        });

        clearHistoryBtn.addActionListener(e -> {
            int selectedTab = tabbedPane.getSelectedIndex();
            String target = "ALL";
            if (selectedTab != 0) {
                java.awt.Component comp = tabbedPane.getTabComponentAt(selectedTab);
                if (comp instanceof ClosableTabComponent) {
                    target = ((ClosableTabComponent) comp).getTargetId();
                }
            }
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
        JMenuItem viewMembersItem = new JMenuItem("View Members");
        JMenuItem leaveItem = new JMenuItem("Leave Group");
        groupPopup.add(viewMembersItem);
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

        viewMembersItem.addActionListener(e -> {
            String selected = groupList.getSelectedValue();
            if (selected == null)
                return;

            // regex
            int groupID = Integer.parseInt(selected.replaceAll(".*\\((\\d+)\\)", "$1"));
            controller.requestGroupMembers(groupID);
        });

        leaveItem.addActionListener(e -> {
            String selected = groupList.getSelectedValue();

            if (selected == null)
                return;

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to leave this group?",
                    "Confirm Leave Group",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                // regex
                int groupID = Integer.parseInt(selected.replaceAll(".*\\((\\d+)\\)", "$1"));
                controller.leaveGroup(groupID);
            }
        });
    }

    public void updateMessageDeleted(int messageId, String sender) {
        SwingUtilities.invokeLater(() -> {
            replaceMessageHTML(publicChatArea, messageId, sender);

            for (JTextPane area : chatTabsMap.values()) {
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
                pane = chatTabsMap.get(target);
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
                String align = sender.equals("YOU") ? "right" : "left";
                String replacement = String.format(
                        "<div id='msg-%d' align='%s' style='text-align: %s;'><b>[%s]</b>: <i>[This message was deleted]</i></div>",
                        messageId, align, align, sender);

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

                // Prevent duplicate rendering: check if element with same id already exists
                String idStr = extractElementId(msg);
                if (idStr != null && hasElementWithId(doc, idStr)) {
                    return; // Skip duplicate message
                }

                kit.insertHTML(doc, doc.getLength(), "<div>" + msg + "</div>", 0, 0, null);
                pane.setCaretPosition(doc.getLength());

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private boolean hasElementWithId(HTMLDocument doc, String id) {
        Element root = doc.getDefaultRootElement();
        return checkElementForId(root, id);
    }

    private boolean checkElementForId(Element elem, String id) {
        Object idAttr = elem.getAttributes().getAttribute(javax.swing.text.html.HTML.Attribute.ID);
        if (idAttr != null && idAttr.toString().equals(id)) {
            return true;
        }
        int childCount = elem.getElementCount();
        for (int i = 0; i < childCount; i++) {
            if (checkElementForId(elem.getElement(i), id)) {
                return true;
            }
        }
        return false;
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
            userDisplayNameMap.clear();

            for (String onlineUser : onlineUserList) {
                if (onlineUser.contains(":")) {
                    String[] parts = onlineUser.split(":", 2);
                    String displayName = parts[0];
                    String username = parts[1];
                    userDisplayNameMap.put(username, displayName);
                    userModel.addElement(onlineUser);
                } else {
                    userModel.addElement(onlineUser);
                }
            }
        });
    }

    public void appendMessageToTab(String targetId, String text) {
        SwingUtilities.invokeLater(() -> {
            if (historyLoadedMap.containsKey(targetId) && !historyLoadedMap.get(targetId)) {
                pendingMessagesMap.get(targetId).add(text);
                return;
            }

            JTextPane area = chatTabsMap.get(targetId);
            if (area == null)
                return;

            appendToPane(area, text);
            area.setCaretPosition(area.getDocument().getLength());
        });
    }

    public void appendHistoryToTab(String targetId, String text) {
        SwingUtilities.invokeLater(() -> {
            JTextPane area = chatTabsMap.get(targetId);
            if (area == null)
                return;
            appendToPane(area, text);
            area.setCaretPosition(area.getDocument().getLength());
        });
    }

    public void setHistoryLoaded(String targetId) {
        setHistoryLoaded(targetId, new java.util.HashSet<>());
    }

    public void setHistoryLoaded(String targetId, java.util.Set<String> renderedElementIds) {
        SwingUtilities.invokeLater(() -> {
            historyLoadedMap.put(targetId, true);
            List<String> pending = pendingMessagesMap.get(targetId);
            if (pending != null && !pending.isEmpty()) {
                JTextPane area = chatTabsMap.get(targetId);
                if (area != null) {
                    for (String pendingMsg : pending) {
                        // Skip messages/files whose IDs were already rendered from history
                        if (!renderedElementIds.isEmpty()) {
                            String idStr = extractElementId(pendingMsg);
                            if (idStr != null && renderedElementIds.contains(idStr)) {
                                continue; // already shown, skip
                            }
                        }
                        appendToPane(area, pendingMsg);
                    }
                    area.setCaretPosition(area.getDocument().getLength());
                }
                pending.clear();
            }
        });
    }

    // Extract element id from HTML string (e.g. id='msg-123' -> "msg-123", id='file-abc' -> "file-abc")
    private String extractElementId(String msg) {
        if (msg.contains("id='")) {
            int start = msg.indexOf("id='") + 4;
            int end = msg.indexOf("'", start);
            if (end > start) return msg.substring(start, end);
        } else if (msg.contains("id=\"")) {
            int start = msg.indexOf("id=\"") + 4;
            int end = msg.indexOf("\"", start);
            if (end > start) return msg.substring(start, end);
        }
        return null;
    }

    // Old method maintained for compatibility (if needed) but redirected
    public void displayGroupMessage(int groupId, String text) {
        appendMessageToTab(String.valueOf(groupId), text);
    }

    public void ensureTabOpen(String targetId) {
        if (chatTabsMap.containsKey(targetId))
            return;

        String title = "👤 " + targetId;
        if (targetId.matches("\\d+")) {
            for (int i = 0; i < groupModel.getSize(); i++) {
                String entry = groupModel.getElementAt(i);
                if (entry.endsWith("(" + targetId + ")")) {
                    int idStart = entry.lastIndexOf('(');
                    title = entry.substring(0, idStart).trim();
                    break;
                }
            }
        } else {
            String dispName = userDisplayNameMap.get(targetId);
            if (dispName != null) {
                title = "👤 " + dispName;
            }
        }
        openTab(title, targetId);
    }

    public void openTab(String title, String targetId) {
        SwingUtilities.invokeLater(() -> {
            if (chatTabsMap.containsKey(targetId)) {
                JTextPane area = chatTabsMap.get(targetId);
                for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                    if (tabbedPane.getComponentAt(i) instanceof JScrollPane) {
                        JScrollPane scrollPane = (JScrollPane) tabbedPane.getComponentAt(i);
                        if (scrollPane.getViewport().getView() == area) {
                            tabbedPane.setSelectedIndex(i);
                            return;
                        }
                    }
                }
                return;
            }

            JTextPane area = new JTextPane();
            area.setEditable(false);
            area.setContentType("text/html");

            area.addHyperlinkListener(e -> {
                if (e.getEventType() == EventType.ACTIVATED) {
                    String href = e.getDescription();
                    if (href.startsWith("del:")) {
                        int messageId = Integer.parseInt(href.split(":")[1]);
                        controller.deleteMessage(messageId, targetId);
                    } else {
                        controller.requestFileDownload(href);
                    }
                }
            });

            JScrollPane scrollPane = new JScrollPane(area);
            tabbedPane.addTab(title, scrollPane);

            int index = tabbedPane.getTabCount() - 1;
            tabbedPane.setTabComponentAt(index, new ClosableTabComponent(title, targetId, this));
            tabbedPane.setSelectedIndex(index);

            chatTabsMap.put(targetId, area);
            historyLoadedMap.put(targetId, false);
            pendingMessagesMap.put(targetId, new java.util.ArrayList<>());
            controller.loadHistory(targetId, 0);
        });
    }

    // groupList = "study:3,game:4"
    public void updateGroupList(String groupList) {
        SwingUtilities.invokeLater(() -> {
            groupModel.clear();

            if (groupList == null || groupList.isEmpty())
                return;

            String[] groupListParts = groupList.split(",");
            for (String entry : groupListParts) {
                String[] parts = entry.split(":");
                String name = parts[0];
                int id = Integer.parseInt(parts[1]);

                // update WEST list (no tabs are automatically opened!)
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
        if (saveResult != JFileChooser.APPROVE_OPTION) {
            hideProgress();
            return;
        }

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
            } finally {
                hideProgress();
            }

        }).start();

    }

    public void showProgress(String text) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(text);
            statusPanel.setVisible(true);
            this.revalidate();
            this.repaint();
        });
    }

    public void hideProgress() {
        SwingUtilities.invokeLater(() -> {
            statusPanel.setVisible(false);
            this.revalidate();
            this.repaint();
        });
    }

    public void showGroupMembers(String groupId, String membersData) {
        String groupName = "Group " + groupId;
        // resolve group name from UI list model
        for (int i = 0; i < groupModel.getSize(); i++) {
            String entry = groupModel.getElementAt(i);
            if (entry.endsWith("(" + groupId + ")")) {
                int idStart = entry.lastIndexOf('(');
                // "📂 groupname (groupId)" -> extract groupname
                String temp = entry.substring(0, idStart).trim(); // "📂 groupname"
                int spaceIndex = temp.indexOf(' ');
                if (spaceIndex != -1) {
                    groupName = temp.substring(spaceIndex + 1).trim();
                } else {
                    groupName = temp;
                }
                break;
            }
        }

        final String finalGroupName = groupName;
        SwingUtilities.invokeLater(() -> {
            new GroupMembersDialog(this, finalGroupName, membersData).setVisible(true);
        });
    }

    public void closeTab(ClosableTabComponent tabComponent) {
        int i = tabbedPane.indexOfTabComponent(tabComponent);
        if (i != -1) {
            tabbedPane.remove(i);
            String targetId = tabComponent.getTargetId();
            chatTabsMap.remove(targetId);
            historyLoadedMap.remove(targetId);
            pendingMessagesMap.remove(targetId);
        }
    }

    private void sendMessageAction() {
        String text = inputField.getText();
        if (text == null || text.trim().isEmpty())
            return;

        // read the active tab to decide routing
        int selectedTab = tabbedPane.getSelectedIndex();
        if (selectedTab == 0) { // Public Chat tab
            this.controller.sendMessages(text);
        } else {
            java.awt.Component comp = tabbedPane.getTabComponentAt(selectedTab);
            if (comp instanceof ClosableTabComponent) {
                String targetId = ((ClosableTabComponent) comp).getTargetId();
                if (targetId.matches("\\d+")) {
                    controller.sendGroupMessage(Integer.parseInt(targetId), text);
                } else {
                    controller.sendPrivateMessage(targetId, text);
                }
            }
        }

        inputField.setText("");
        inputField.requestFocus();
    }
}
