package org.docear.plugin.ai.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * AI 聊天顶部状态栏。
 */
public class AiChatStatusBar extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel statusLabel;
    private AiChatContextInfo contextInfo;

    public AiChatStatusBar() {
        super(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        statusLabel = new JLabel("\u52a0\u8f7d\u4e2d...");
        statusLabel.setFont(statusLabel.getFont().deriveFont(11f));
        statusLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        statusLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                showDetails();
            }
        });
        add(statusLabel, BorderLayout.CENTER);
    }

    public void updateContext(AiChatContextInfo info) {
        this.contextInfo = info;
        if (info == null) {
            statusLabel.setText("\u672a\u52a0\u8f7d\u5bfc\u56fe");
            statusLabel.setForeground(Color.DARK_GRAY);
            return;
        }
        statusLabel.setText(info.formatStatusLine());
        if (info.isQuotaExceeded()) {
            statusLabel.setForeground(new Color(180, 30, 30));
        } else if (info.isQuotaNear()) {
            statusLabel.setForeground(new Color(180, 120, 0));
        } else {
            statusLabel.setForeground(info.isBackendReady() ? new Color(30, 100, 30) : new Color(160, 60, 0));
        }
    }

    public void setHint(String hint) {
        if (contextInfo == null) {
            statusLabel.setText(hint);
            return;
        }
        updateContext(new AiChatContextInfo(
                contextInfo.getMapTitle(),
                contextInfo.getMapPath(),
                contextInfo.getSelectedNodeText(),
                contextInfo.getFilesIncluded(),
                contextInfo.getFilesDiscovered(),
                contextInfo.isBackendReady(),
                contextInfo.getRedactionCount(),
                hint,
                contextInfo.getMonthlyUsageCount(),
                contextInfo.getMonthlyQuota(),
                contextInfo.getTodayUsageCount()));
    }

    private void showDetails() {
        if (contextInfo == null) {
            return;
        }
        JOptionPane.showMessageDialog(this, contextInfo.formatDetailText(),
                "AI \u4e0a\u4e0b\u6587\u8be6\u60c5", JOptionPane.INFORMATION_MESSAGE);
    }
}
