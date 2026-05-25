package client.view;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.DefaultListCellRenderer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

public class GroupMembersDialog extends JDialog {

    public GroupMembersDialog(ChatView parent, String groupName, String membersData) {
        super(parent, "Group Members - " + groupName, true);
        setSize(320, 400);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel("Members of " + groupName);
        titleLabel.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        if (membersData != null && !membersData.isEmpty()) {
            String[] parts = membersData.split(",");
            for (String entry : parts) {
                listModel.addElement(entry);
            }
        } else {
            listModel.addElement("No members found");
        }

        JList<String> list = new JList<>(listModel);
        list.setFixedCellHeight(26);
        list.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Use a custom cell renderer to dynamically set font and text color
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String entry = value.toString();
                
                if (entry.equals("No members found")) {
                    label.setForeground(Color.GRAY);
                    return label;
                }
                
                String[] memberInfo = entry.split(":");
                String name = memberInfo[0];
                String status = memberInfo.length > 1 ? memberInfo[1] : "offline";
                
                if ("online".equalsIgnoreCase(status)) {
                    label.setText("● " + name + " (online)");
                    label.setForeground(new Color(46, 204, 113)); // Emerald green
                    label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
                } else {
                    label.setText("○ " + name + " (offline)");
                    label.setForeground(new Color(149, 165, 166)); // Slate gray
                    label.setFont(label.getFont().deriveFont(java.awt.Font.ITALIC));
                }
                
                return label;
            }
        });

        JScrollPane scrollPane = new JScrollPane(list);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton closeBtn = new JButton("Close");
        closeBtn.setPreferredSize(new Dimension(100, 32));
        closeBtn.addActionListener(e -> dispose());
        bottomPanel.add(closeBtn);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        this.add(mainPanel, BorderLayout.CENTER);
    }
}
