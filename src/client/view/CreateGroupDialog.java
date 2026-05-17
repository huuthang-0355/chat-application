package client.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import client.controller.ChatController;

public class CreateGroupDialog extends JDialog {
    private JTextField groupNameField;
    private JLabel statusLabel;

    public CreateGroupDialog(ChatView parent, ChatController chatController) {
        super(parent, "Create New Group", true); // true = modal
        setSize(300, 180);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        // mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5); // padding

        // group name
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.4;
        form.add(new JLabel("Group Name:"), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1;
        groupNameField = new JTextField(20);
        form.add(groupNameField, gbc);

        // status
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.4;
        form.add(new JLabel("Status: "), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        statusLabel = new JLabel(" ");
        form.add(statusLabel, gbc);

        mainPanel.add(form, BorderLayout.CENTER);

        JPanel ctaPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton createBtn = new JButton("Create");
        createBtn.setPreferredSize(new Dimension(120, 35));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setPreferredSize(new Dimension(120, 35));

        ctaPanel.add(createBtn);
        ctaPanel.add(cancelBtn);

        mainPanel.add(ctaPanel, BorderLayout.SOUTH);

        this.add(mainPanel, BorderLayout.CENTER);

        createBtn.addActionListener(e -> {
            String name = groupNameField.getText().trim();

            if (name.isEmpty()) {
                statusLabel.setText("Name cannot be empty");
                return;
            }

            chatController.createGroup(name);
            dispose(); // close dialog
        });

        cancelBtn.addActionListener(e -> dispose());
    }
}
