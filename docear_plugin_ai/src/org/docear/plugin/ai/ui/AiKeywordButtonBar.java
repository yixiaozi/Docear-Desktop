package org.docear.plugin.ai.ui;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * 关键词库快捷按钮条。
 */
public class AiKeywordButtonBar extends JPanel {

    private static final long serialVersionUID = 1L;

    public interface KeywordActionListener {
        void onKeywordClicked(String keyword, boolean sendImmediately);
    }

    private KeywordActionListener listener;

    public AiKeywordButtonBar() {
        super(new FlowLayout(FlowLayout.LEFT, 4, 2));
    }

    public void setKeywordActionListener(KeywordActionListener listener) {
        this.listener = listener;
    }

    public void setKeywords(List<String> keywords) {
        removeAll();
        if (keywords == null || keywords.isEmpty()) {
            JButton empty = new JButton("(\u65e0\u5173\u952e\u8bcd)");
            empty.setEnabled(false);
            add(empty);
        } else {
            for (int i = 0; i < keywords.size(); i++) {
                add(createKeywordButton(keywords.get(i)));
            }
        }
        revalidate();
        repaint();
    }

    private JButton createKeywordButton(final String keyword) {
        final JButton button = new JButton(keyword);
        button.setFont(button.getFont().deriveFont(11f));
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (listener != null) {
                    listener.onKeywordClicked(keyword, false);
                }
            }
        });
        return button;
    }
}
