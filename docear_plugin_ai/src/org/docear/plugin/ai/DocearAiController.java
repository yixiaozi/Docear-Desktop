package org.docear.plugin.ai;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JTabbedPane;

import org.docear.plugin.ai.actions.AiGenerateSubNodesAction;
import org.docear.plugin.ai.backend.AiBackend;
import org.docear.plugin.ai.backend.CopilotCliBackend;
import org.docear.plugin.ai.ui.AiChatSidebar;
import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.ui.IMenuContributor;
import org.freeplane.core.ui.MenuBuilder;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;

public class DocearAiController {

    private static DocearAiController instance;
    private final ModeController modeController;
    private final AiBackend backend;
    private final AiChatSidebar chatSidebar;

    private DocearAiController(ModeController modeController) {
        this.modeController = modeController;
        this.backend = createBackend();
        this.chatSidebar = new AiChatSidebar();
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
                // 1. 节点右键菜单
                builder.addSeparator("/node_popup", MenuBuilder.AS_CHILD);
                builder.addAction("/node_popup", modeController.getAction(AiGenerateSubNodesAction.KEY), MenuBuilder.AS_CHILD);

                // 2. 主菜单「Extras > AI 聊天侧边栏」—— 点击后添加到右侧 Tab
                builder.addAction("/menu_bar/extras", new AFreeplaneAction("AiChatSidebarAction", "AI 聊天侧边栏", null) {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        showAiChatSidebar();
                    }
                }, MenuBuilder.AS_CHILD);
            }
        });
        LogUtils.info("Docear AI menus registered.");
    }

    /**
     * 将 AI 聊天侧边栏添加到当前思维导图的右侧 Tab 中。
     */
    private void showAiChatSidebar() {
        try {
            final Component mapView = Controller.getCurrentController().getMapViewManager().getMapViewComponent();
            if (mapView == null) {
                LogUtils.warn("No map view available for AI sidebar.");
                return;
            }

            // 向上遍历找到右侧的 JTabbedPane
            Container parent = mapView.getParent();
            JTabbedPane rightTabs = null;
            while (parent != null) {
                if (parent instanceof JTabbedPane) {
                    // 简单判断：通常右侧的 Tab 面板会有多个子 Tab
                    JTabbedPane tab = (JTabbedPane) parent;
                    if (tab.getTabCount() >= 0) {
                        rightTabs = tab;
                        break;
                    }
                }
                parent = parent.getParent();
            }

            if (rightTabs != null) {
                // 避免重复添加
                for (int i = 0; i < rightTabs.getTabCount(); i++) {
                    if (rightTabs.getComponentAt(i) == chatSidebar) {
                        rightTabs.setSelectedIndex(i);
                        chatSidebar.switchToMap(Controller.getCurrentController().getMap());
                        return;
                    }
                }
                rightTabs.addTab("AI Chat", chatSidebar);
                rightTabs.setSelectedComponent(chatSidebar);
                chatSidebar.switchToMap(Controller.getCurrentController().getMap());
                LogUtils.info("AI Chat Sidebar added to right tab.");
            } else {
                LogUtils.warn("Could not find right tab pane for AI sidebar.");
            }
        } catch (Exception ex) {
            LogUtils.severe("Failed to show AI Chat Sidebar: " + ex.getMessage());
        }
    }

    public void generateSubNodesForNode(NodeModel node) {
        // TODO: 调用 backend 生成子节点
    }
}
