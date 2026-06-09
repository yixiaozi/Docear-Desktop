package org.docear.plugin.ai;

import javax.swing.JTabbedPane;

import org.docear.plugin.ai.actions.AiGenerateSubNodesAction;
import org.docear.plugin.ai.backend.AiBackend;
import org.docear.plugin.ai.backend.CopilotCliBackend;
import java.util.List;

import org.docear.plugin.ai.log.AiInteractionLogger;
import org.docear.plugin.ai.log.AiInteractionRecord;
import org.docear.plugin.ai.prompt.AiPromptBuilder;
import org.docear.plugin.ai.ui.AiChatSidebar;
import org.docear.plugin.ai.ui.AiChatTabInstaller;
import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.ui.IMenuContributor;
import org.freeplane.core.ui.MenuBuilder;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;

public class DocearAiController {

    private static DocearAiController instance;
    private final ModeController modeController;
    private final AiBackend backend;
    private final AiChatSidebar chatSidebar;
    private final AiPromptBuilder promptBuilder;
    private final AiInteractionLogger interactionLogger;

    private DocearAiController(ModeController modeController) {
        this.modeController = modeController;
        this.backend = createBackend();
        this.promptBuilder = new AiPromptBuilder();
        this.interactionLogger = new AiInteractionLogger();
        this.promptBuilder.getTemplateStore().ensureTemplateFileExists();
        this.interactionLogger.ensureLogDirectoryExists();
        this.chatSidebar = new AiChatSidebar(this);
        registerActions();
        registerMenus();
        installAiChatTab();
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

    public AiPromptBuilder getPromptBuilder() {
        return promptBuilder;
    }

    public AiInteractionLogger getInteractionLogger() {
        return interactionLogger;
    }

    public String invokeChat(String userInput, MapModel map) {
        String prompt = promptBuilder.buildChatPrompt(userInput, map);
        String response = backend.chat(prompt);
        logInteraction(AiInteractionRecord.TYPE_CHAT, userInput, prompt, response, map);
        return response;
    }

    public List<String> invokeGenerateSubNodes(String topic, MapModel map, int count) {
        String prompt = promptBuilder.buildSubNodesPrompt(topic, map, count);
        List<String> result = backend.generateSubNodes(prompt, count);
        String response = joinLines(result);
        logInteraction(AiInteractionRecord.TYPE_GENERATE_SUBNODES, topic, prompt, response, map);
        return result;
    }

    private void logInteraction(String type, String userInput, String prompt, String response, MapModel map) {
        String mapPath = AiPromptBuilder.resolveMapPath(map);
        String mapTitle = map != null && map.getTitle() != null ? map.getTitle() : "\u65e0";
        interactionLogger.log(new AiInteractionRecord(
                type, userInput, prompt, response, mapPath, mapTitle, System.currentTimeMillis()));
    }

    private String joinLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(lines.get(i));
        }
        return sb.toString();
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

    private void installAiChatTab() {
        AiChatTabInstaller.install(modeController, chatSidebar);
    }

    /**
     * 切换到 AI 聊天 Tab（若尚未安装则尝试安装）。
     */
    private void showAiChatSidebar() {
        try {
            JTabbedPane rightTabs = AiChatTabInstaller.findFormatTabbedPane(modeController);
            if (rightTabs == null) {
                AiChatTabInstaller.tryInstall(modeController, chatSidebar);
                rightTabs = AiChatTabInstaller.findFormatTabbedPane(modeController);
            }
            if (rightTabs == null) {
                LogUtils.warn("Could not find right tab pane for AI sidebar.");
                return;
            }

            final String tabTitle = AiChatTabInstaller.getTabTitle();
            for (int i = 0; i < rightTabs.getTabCount(); i++) {
                if (tabTitle.equals(rightTabs.getTitleAt(i))) {
                    rightTabs.setSelectedIndex(i);
                    chatSidebar.switchToMap(Controller.getCurrentController().getMap());
                    return;
                }
            }

            AiChatTabInstaller.tryInstall(modeController, rightTabs, chatSidebar);
            for (int i = 0; i < rightTabs.getTabCount(); i++) {
                if (tabTitle.equals(rightTabs.getTitleAt(i))) {
                    rightTabs.setSelectedIndex(i);
                    chatSidebar.switchToMap(Controller.getCurrentController().getMap());
                    return;
                }
            }
            LogUtils.warn("AI chat tab could not be selected after install attempt.");
        } catch (Exception ex) {
            LogUtils.severe("Failed to show AI Chat Sidebar: " + ex.getMessage());
        }
    }

    public void generateSubNodesForNode(NodeModel node) {
        // TODO: 调用 backend 生成子节点
    }
}
