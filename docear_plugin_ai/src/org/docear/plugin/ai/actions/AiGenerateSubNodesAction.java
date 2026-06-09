package org.docear.plugin.ai.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JOptionPane;

import org.docear.plugin.ai.DocearAiController;
import org.docear.plugin.ai.backend.AiBackend;
import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.text.TextController;
import org.freeplane.features.text.mindmapmode.MTextController;

/**
 * AI 生成子节点动作。
 * 右键菜单调用此动作，使用当前选中的节点内容作为提示词，生成子节点并插入到思维导图。
 */
public class AiGenerateSubNodesAction extends AFreeplaneAction {

    private static final long serialVersionUID = 1L;
    public static final String KEY = "AiGenerateSubNodesAction";

    public AiGenerateSubNodesAction() {
        super(KEY, "AI: 生成子节点", null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        NodeModel selectedNode = Controller.getCurrentController().getSelection().getSelected();
        if (selectedNode == null) {
            LogUtils.warn("No node selected for AI generation.");
            return;
        }

        String nodeText = TextController.getController().getPlainTextContent(selectedNode);
        if (nodeText == null || nodeText.trim().isEmpty()) {
            nodeText = "未命名主题";
        }

        LogUtils.info("Calling AI to generate sub-nodes for: " + nodeText);

        AiBackend backend = DocearAiController.getController().getBackend();

        if (!backend.isAvailable()) {
            JOptionPane.showMessageDialog(
                null,
                "未检测到 GitHub Copilot CLI。\n\n请先安装并登录：\n1. npm install -g @github/copilot\n2. 运行 copilot 并执行 /login",
                "AI 功能不可用",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        List<String> subNodes = backend.generateSubNodes(nodeText, 5);

        if (subNodes.isEmpty()) {
            JOptionPane.showMessageDialog(
                null,
                "AI 未能生成子节点。\n\n可能原因：\n- Copilot CLI 未正确登录\n- 网络连接问题\n- 请求超时\n\n请在终端执行 'copilot --version' 验证。",
                "生成失败",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // 将生成的子节点插入到当前节点下
        MMapController mapController = (MMapController) Controller.getCurrentModeController().getMapController();
        MTextController textController = (MTextController) TextController.getController();
        for (String title : subNodes) {
            NodeModel newNode = mapController.addNewNode(selectedNode, selectedNode.getChildCount(), true);
            textController.setNodeText(newNode, title);
        }

        LogUtils.info("Successfully inserted " + subNodes.size() + " sub-nodes.");
    }
}
