package org.docear.plugin.ai.prompt;

import java.io.File;

import javax.swing.JOptionPane;

import org.docear.plugin.ai.DocearAiConfig;
import org.freeplane.features.map.IMapChangeListener;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;

/**
 * 防止删除 AI 提示词导图中的系统节点（绿色背景节点）。
 */
public final class AiPromptTemplateGuard implements IMapChangeListener {

    private final File templateFile;

    public AiPromptTemplateGuard() {
        this(new File(new DocearAiConfig().getPromptTemplateFile()));
    }

    public AiPromptTemplateGuard(File templateFile) {
        this.templateFile = templateFile;
    }

    public static void install(ModeController modeController) {
        modeController.getMapController().addMapChangeListener(new AiPromptTemplateGuard());
    }

    public void onPreNodeMoved(NodeModel oldParent, int oldIndex, NodeModel newParent, NodeModel child, int newIndex) {
    }

    public void onPreNodeDelete(NodeModel oldParent, NodeModel selectedNode, int index) {
    }

    public void onNodeMoved(NodeModel oldParent, int oldIndex, NodeModel newParent, NodeModel child, int newIndex) {
    }

    public void onNodeInserted(NodeModel parent, NodeModel child, int newIndex) {
    }

    public void onNodeDeleted(NodeModel parent, NodeModel child, int index) {
        if (parent == null || child == null) {
            return;
        }
        MapModel map = child.getMap();
        if (!AiPromptTemplateNodes.isPromptTemplateMap(map, templateFile)) {
            return;
        }
        if (!AiPromptTemplateNodes.isProtectedNode(child)) {
            return;
        }

        MapController mapController = Controller.getCurrentModeController().getMapController();
        mapController.insertNodeIntoWithoutUndo(child, parent, index);
        JOptionPane.showMessageDialog(
                null,
                "\u6b64\u8282\u70b9\u4e3a\u7cfb\u7edf\u8282\u70b9\uff0c\u4e0d\u53ef\u5220\u9664\u3002\n"
                        + "\uff08\u7eff\u8272\u80cc\u666f\u8282\u70b9\uff1a"
                        + AiPromptTemplateNodes.getProtectedNodeDescription() + "\uff09",
                "AI \u63d0\u793a\u8bcd",
                JOptionPane.WARNING_MESSAGE);
    }

    public void mapChanged(MapChangeEvent event) {
    }
}
