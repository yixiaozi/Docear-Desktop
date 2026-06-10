package org.docear.plugin.ai.actions;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import org.docear.plugin.ai.DocearAiController;
import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;

/**
 * 右键菜单：针对选中节点打开 AI 聊天并预填提问上下文。
 */
public class AiAskAboutNodeAction extends AFreeplaneAction {

    private static final long serialVersionUID = 1L;
    public static final String KEY = "AiAskAboutNodeAction";

    public AiAskAboutNodeAction() {
        super(KEY, "AI: \u5bf9\u6b64\u8282\u70b9\u63d0\u95ee", null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        NodeModel selectedNode = Controller.getCurrentController().getSelection().getSelected();
        if (selectedNode == null) {
            JOptionPane.showMessageDialog(null,
                    "\u8bf7\u5148\u9009\u4e2d\u4e00\u4e2a\u8282\u70b9\u3002",
                    "AI \u63d0\u95ee",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        DocearAiController controller = DocearAiController.getController();
        if (controller == null) {
            LogUtils.warn("DocearAiController is not initialized.");
            return;
        }

        LogUtils.info("Opening AI chat for node: " + selectedNode.getText());
        controller.askAboutNode(selectedNode);
    }
}
