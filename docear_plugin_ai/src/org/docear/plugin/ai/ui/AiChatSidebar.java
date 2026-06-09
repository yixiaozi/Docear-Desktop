package org.docear.plugin.ai.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.MapModel;

/**
 * AI 聊天侧边栏。
 * 每个思维导图独立维护一个聊天会话。
 */
public class AiChatSidebar extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JTextArea chatArea;
    private final JTextField inputField;
    private final JButton sendButton;

    private MapModel currentMap;
    // TODO: 后续绑定 AiChatSession

    public AiChatSidebar() {
        setLayout(new BorderLayout());

        // 聊天记录显示区
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        // 输入区
        inputField = new JTextField();
        sendButton = new JButton("发送");

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        // 事件绑定
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        LogUtils.info("AiChatSidebar initialized.");
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) {
            return;
        }

        chatArea.append("你: " + text + "\n");
        inputField.setText("");

        // TODO: 调用 CopilotCliBackend 获取回复
        // 目前先模拟回复
        chatArea.append("AI: [功能开发中，敬请期待...]\n\n");
    }

    /**
     * 切换到指定的思维导图时调用，加载对应的聊天历史。
     */
    public void switchToMap(MapModel map) {
        this.currentMap = map;
        chatArea.setText("");
        chatArea.append("已切换到思维导图: " + (map != null ? map.getTitle() : "无") + "\n\n");
        // TODO: 从 AiChatHistoryExtension 恢复聊天记录
    }
}
