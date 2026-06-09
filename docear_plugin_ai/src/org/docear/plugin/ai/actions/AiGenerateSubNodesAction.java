package org.docear.plugin.ai.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import org.docear.plugin.ai.DocearAiController;
import org.docear.plugin.ai.backend.AiBackend;
import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.text.TextController;

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

        String nodeText = TextController.getController().getPlainText(selectedNode);
        if (nodeText == null || nodeText.trim().isEmpty()) {
            nodeText = "未命名主题";
        }

        LogUtils.info("Calling AI to generate sub-nodes for: " + nodeText);

        AiBackend backend = DocearAiController.getController().getBackend();
        List<String> subNodes = backend.generateSubNodes(nodeText, 5);

        if (subNodes.isEmpty()) {
            LogUtils.warn("AI did not return any sub-nodes.");
            return;
        }

        // 将生成的子节点插入到当前节点下
        for (String title : subNodes) {
            NodeModel newNode = Controller.getCurrentModeController().getMapController().addNewNode(selectedNode, selectedNode.getChildCount(), true);
            TextController.getController().setNodeText(newNode, title);
        }

        LogUtils.info("Successfully inserted " + subNodes.size() + " sub-nodes.");
    }
}
