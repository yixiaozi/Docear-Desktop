package org.docear.plugin.ai.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

import org.docear.plugin.ai.chat.AiChatMessage;

/**
 * 单条聊天消息块（用户 / AI）。
 */
public class AiChatMessagePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    public interface MessageActionListener {
        void onInsertAsChild(String content);

        void onInsertAsSibling(String content);
    }

    private final AiChatMessage.Role role;
    private final JTextPane aiContentPane;
    private final JTextArea userContentArea;
    private final JPanel actionPanel;
    private String plainContent = "";

    public AiChatMessagePanel(AiChatMessage.Role role) {
        this.role = role;
        setLayout(new BorderLayout(4, 2));
        setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));

        String title = role == AiChatMessage.Role.USER ? "\u4f60" : "AI";
        Color bg = role == AiChatMessage.Role.USER ? new Color(232, 244, 255) : new Color(245, 255, 240);
        setBackground(bg);
        setOpaque(true);

        JLabel header = new JLabel(title);
        header.setBorder(BorderFactory.createEmptyBorder(0, 4, 2, 4));
        header.setFont(header.getFont().deriveFont(java.awt.Font.BOLD, 11f));

        userContentArea = new JTextArea();
        userContentArea.setEditable(false);
        userContentArea.setLineWrap(true);
        userContentArea.setWrapStyleWord(true);
        userContentArea.setBackground(bg);
        userContentArea.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        aiContentPane = new JTextPane();
        aiContentPane.setEditable(false);
        aiContentPane.setContentType("text/html");
        aiContentPane.setBackground(bg);
        aiContentPane.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actionPanel.setOpaque(false);

        add(header, BorderLayout.NORTH);
        if (role == AiChatMessage.Role.USER) {
            add(userContentArea, BorderLayout.CENTER);
        } else {
            add(aiContentPane, BorderLayout.CENTER);
            buildAssistantActions(null);
            add(actionPanel, BorderLayout.SOUTH);
        }
    }

    private void buildAssistantActions(final MessageActionListener listener) {
        actionPanel.removeAll();
        JButton copyButton = new JButton("\u590d\u5236");
        copyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                copyToClipboard(plainContent);
            }
        });
        actionPanel.add(copyButton);

        if (listener != null) {
            JButton childButton = new JButton("\u63d2\u5165\u5b50\u8282\u70b9");
            childButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    listener.onInsertAsChild(plainContent);
                }
            });
            JButton siblingButton = new JButton("\u63d2\u5165\u5144\u5f1f\u8282\u70b9");
            siblingButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    listener.onInsertAsSibling(plainContent);
                }
            });
            actionPanel.add(childButton);
            actionPanel.add(siblingButton);
        }
        actionPanel.setVisible(role == AiChatMessage.Role.ASSISTANT);
    }

    public void setAssistantActionListener(MessageActionListener listener) {
        if (role == AiChatMessage.Role.ASSISTANT) {
            buildAssistantActions(listener);
            revalidate();
            repaint();
        }
    }

    public void setContent(String content) {
        plainContent = content != null ? content : "";
        if (role == AiChatMessage.Role.USER) {
            userContentArea.setText(plainContent);
        } else {
            aiContentPane.setText(AiMarkdownRenderer.toHtml(plainContent));
            aiContentPane.setCaretPosition(0);
        }
    }

    public void appendStreamingChunk(String chunk) {
        if (role != AiChatMessage.Role.ASSISTANT || chunk == null) {
            return;
        }
        plainContent = plainContent + chunk;
        final String html = AiMarkdownRenderer.toHtml(plainContent);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                aiContentPane.setText(html);
                aiContentPane.setCaretPosition(aiContentPane.getDocument().getLength());
            }
        });
    }

    public String getPlainContent() {
        return plainContent;
    }

    public boolean isAssistant() {
        return role == AiChatMessage.Role.ASSISTANT;
    }

    public static JScrollPane wrap(AiChatMessagePanel panel) {
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        return scroll;
    }

    private static void copyToClipboard(String text) {
        if (text == null) {
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(text), null);
    }
}
