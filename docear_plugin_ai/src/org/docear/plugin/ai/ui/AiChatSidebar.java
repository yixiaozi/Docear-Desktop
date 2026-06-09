package org.docear.plugin.ai.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.docear.plugin.ai.DocearAiController;
import org.docear.plugin.ai.backend.AiBackend;
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

    public AiChatSidebar() {
        setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        inputField = new JTextField();
        sendButton = new JButton("发送");

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        ActionListener sendListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        };
        sendButton.addActionListener(sendListener);
        inputField.addActionListener(sendListener);

        LogUtils.info("AiChatSidebar initialized.");
    }

    private void sendMessage() {
        final String text = inputField.getText().trim();
        if (text.isEmpty()) {
            return;
        }

        chatArea.append("你: " + text + "\n");
        inputField.setText("");
        setInputEnabled(false);
        chatArea.append("AI: 思考中...\n");

        final Thread worker = new Thread(new Runnable() {
            public void run() {
                final String reply = requestAiReply(text);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        replaceLastLine("AI: 思考中...", "AI: " + formatReply(reply) + "\n\n");
                        setInputEnabled(true);
                        inputField.requestFocusInWindow();
                    }
                });
            }
        }, "AiChatWorker");
        worker.setDaemon(true);
        worker.start();
    }

    private String requestAiReply(String text) {
        try {
            DocearAiController controller = DocearAiController.getController();
            if (controller == null) {
                return "AI 控制器未初始化。";
            }
            AiBackend backend = controller.getBackend();
            if (!backend.isAvailable()) {
                return "未检测到 Copilot CLI。请先安装并登录：\n1. npm install -g @github/copilot\n2. 运行 copilot 并执行 /login";
            }
            String reply = backend.chat(text);
            if (reply == null || reply.trim().length() == 0) {
                return "未收到有效回复。请在 PowerShell 中测试：copilot -p \"测试\" -s";
            }
            return reply;
        } catch (Exception e) {
            LogUtils.severe("AI chat failed: " + e.getMessage());
            return "调用失败: " + e.getMessage();
        }
    }

    private void replaceLastLine(String oldLine, String newLine) {
        String content = chatArea.getText();
        int index = content.lastIndexOf(oldLine);
        if (index >= 0) {
            chatArea.setText(content.substring(0, index) + newLine + content.substring(index + oldLine.length()));
        } else {
            chatArea.append(newLine);
        }
    }

    private String formatReply(String reply) {
        return reply.replace("\r\n", "\n").replace("\r", "\n");
    }

    private void setInputEnabled(boolean enabled) {
        inputField.setEnabled(enabled);
        sendButton.setEnabled(enabled);
    }

    public void switchToMap(MapModel map) {
        this.currentMap = map;
        chatArea.setText("");
        chatArea.append("已切换到思维导图: " + (map != null ? map.getTitle() : "无") + "\n\n");
    }
}
