package org.docear.plugin.ai;

import org.docear.plugin.ai.actions.AiGenerateSubNodesAction;
import org.docear.plugin.ai.backend.AiBackend;
import org.docear.plugin.ai.backend.CopilotCliBackend;
import org.freeplane.core.ui.IMenuContributor;
import org.freeplane.core.ui.MenuBuilder;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.ModeController;

public class DocearAiController {

    private static DocearAiController instance;
    private final ModeController modeController;
    private final AiBackend backend;

    private DocearAiController(ModeController modeController) {
        this.modeController = modeController;
        this.backend = createBackend();
        registerActions();
        registerMenus();
    }

    private AiBackend createBackend() {
        DocearAiConfig config = new DocearAiConfig();
        String backendType = config.getBackendType();
        if ("copilot_cli".equalsIgnoreCase(backendType)) {
            return new CopilotCliBackend();
        }
        // TODO: 未来在这里添加 OpenAI、Ollama 等后端的创建逻辑
        return new CopilotCliBackend();
    }

    public static void install(ModeController modeController) {
        if (instance == null) {
            instance = new DocearAiController(modeController);
        }
    }

    public static DocearAiController getController() {
        return instance;
    }

    public AiBackend getBackend() {
        return backend;
    }

    private void registerActions() {
        modeController.addAction(new AiGenerateSubNodesAction());
        LogUtils.info("Docear AI actions registered.");
    }

    private void registerMenus() {
        modeController.addMenuContributor(new IMenuContributor() {
            @Override
            public void updateMenus(ModeController mc, MenuBuilder builder) {
                // 将 AI 动作直接添加到节点右键菜单（/node_popup）
                builder.addSeparator("/node_popup", MenuBuilder.AS_CHILD);
                builder.addAction("/node_popup", modeController.getAction(AiGenerateSubNodesAction.KEY), MenuBuilder.AS_CHILD);
            }
        });
        LogUtils.info("Docear AI menus registered.");
    }

    public void generateSubNodesForNode(NodeModel node) {
        // TODO: 调用 backend 生成子节点
    }
}
