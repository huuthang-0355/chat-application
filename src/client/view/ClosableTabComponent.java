package client.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ClosableTabComponent extends JPanel {
    private String targetId;
    private ChatView chatView;

    public ClosableTabComponent(String title, String targetId, ChatView chatView) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        this.targetId = targetId;
        this.chatView = chatView;
        setOpaque(false);

        JLabel label = new JLabel(title);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        add(label);

        JButton closeBtn = new JButton("✖");
        closeBtn.setPreferredSize(new Dimension(17, 17));
        closeBtn.setToolTipText("Close tab");
        closeBtn.setContentAreaFilled(false);
        closeBtn.setFocusable(false);
        closeBtn.setBorder(BorderFactory.createEmptyBorder());
        closeBtn.setBorderPainted(false);
        closeBtn.setOpaque(false);
        
        closeBtn.addActionListener(e -> {
            chatView.closeTab(this);
        });

        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                closeBtn.setForeground(Color.RED);
            }
            public void mouseExited(MouseEvent e) {
                closeBtn.setForeground(Color.BLACK);
            }
        });

        add(closeBtn);
    }

    public String getTargetId() {
        return targetId;
    }
}
