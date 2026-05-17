package client.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import client.controller.ChatController;

public class ChatView extends JFrame {
    private ChatController controller;

    private JTabbedPane tabbedPane;
    private Map<Integer, JTextArea> groupChatAreas = new HashMap<>(); // groupId -> JTextArea
    private List<Integer> groupTabIds = new ArrayList<>();

    private JTextArea publicChatArea; // tab 0 - always presnet
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
        publicChatArea = new JTextArea();
        publicChatArea.setEditable(false);
        publicChatArea.setLineWrap(true);
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

    // controller call this func when having new messages from background thread
    // (ReadThread)
    public void displayMessage(String text) {
        // update UI in EDT thread
        SwingUtilities.invokeLater(() -> {
            publicChatArea.append(text + "\n");

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
            JTextArea area = groupChatAreas.get(groupId);

            if (area == null)
                return;

            area.append(text + "\n");
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
                JTextArea area = new JTextArea();
                area.setEditable(false);
                area.setLineWrap(true);
                tabbedPane.addTab(name, new JScrollPane(area));
                groupChatAreas.put(id, area);
                groupTabIds.add(id);

                // update WEST list
                groupModel.addElement("📂 " + name + " (" + id + ")");
            }
        });
    }
}
