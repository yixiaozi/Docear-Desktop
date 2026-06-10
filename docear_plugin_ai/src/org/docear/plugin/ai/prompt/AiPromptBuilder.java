package org.docear.plugin.ai.prompt;



import java.io.File;



import org.docear.plugin.ai.DocearAiConfig;
import org.docear.plugin.ai.chat.AiChatHistoryFormatter;
import org.docear.plugin.ai.chat.AiChatSession;
import org.freeplane.core.util.LogUtils;

import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;



/**

 * 根据模板与当前思维导图上下文构建发送给 CLI 的提示词。

 */

public class AiPromptBuilder {



    private final AiPromptTemplateStore templateStore;



    public AiPromptBuilder() {

        this(new AiPromptTemplateStore());

    }



    public AiPromptBuilder(AiPromptTemplateStore templateStore) {

        this.templateStore = templateStore;

    }



    public AiPromptTemplateStore getTemplateStore() {

        return templateStore;

    }



    public String buildChatPrompt(String userQuestion, MapModel map) {

        return buildChatPrompt(userQuestion, map, null);

    }



    public String buildChatPrompt(String userQuestion, MapModel map, AiChatSession session) {
        return buildChatPrompt(userQuestion, map, session, null);
    }

    public String buildChatPrompt(String userQuestion, MapModel map, AiChatSession session, NodeModel focusNode) {
        templateStore.reloadIfChanged();
        String template = templateStore.getChatTemplate();
        return sanitizeForOutbound(applyChatPlaceholders(template, map, userQuestion, session, focusNode));
    }



    public String buildSubNodesPrompt(String topic, MapModel map, int count) {

        templateStore.reloadIfChanged();

        String template = templateStore.getSubNodesTemplate();

        String prompt = applyChatPlaceholders(template, map, topic, null, null);

        prompt = prompt + "\n\n请生成 " + count + " 个子主题（仅返回标题列表，每行一个，不要解释）。";

        return sanitizeForOutbound(prompt);

    }



    public static String sanitizeForOutbound(String prompt) {

        AiSensitiveDataFilter.FilterResult result = AiSensitiveDataFilter.filter(prompt);

        if (result.getRedactionCount() <= 0) {

            return result.getText();

        }

        return result.getText() + result.getNotice();

    }

    public static int countRedactions(String prompt) {
        return AiSensitiveDataFilter.filter(prompt).getRedactionCount();
    }

    public static String limitPromptLength(String prompt, int maxChars) {
        if (prompt == null || maxChars <= 0 || prompt.length() <= maxChars) {
            return prompt != null ? prompt : "";
        }
        LogUtils.warn("AI prompt truncated from " + prompt.length() + " to " + maxChars + " chars.");
        return prompt.substring(0, maxChars)
                + "\n\n[\u63d0\u793a\u8bcd\u8fc7\u957f\uff0c\u5df2\u622a\u65ad\u81f3 " + maxChars + " \u5b57\u7b26]";
    }

    public String buildRawChatPrompt(String userQuestion, MapModel map, AiChatSession session) {
        return buildRawChatPrompt(userQuestion, map, session, null);
    }

    public String buildRawChatPrompt(String userQuestion, MapModel map, AiChatSession session, NodeModel focusNode) {
        return buildRawChatPrompt(userQuestion, map, session, focusNode, null);
    }

    public String buildRawChatPrompt(String userQuestion, MapModel map, AiChatSession session, NodeModel focusNode,
            AiPromptBuildProgress progress) {
        templateStore.reloadIfChanged();
        String template = templateStore.getChatTemplate();
        return applyChatPlaceholders(template, map, userQuestion, session, focusNode, progress);
    }

    private static void report(AiPromptBuildProgress progress, int step, int total, String label) {
        if (progress != null) {
            progress.onStep(step, total, label);
        }
    }

    static boolean isLightweightQuestion(String question) {
        if (question == null) {
            return false;
        }
        String q = question.trim();
        if (q.length() == 0 || q.length() > 40) {
            return false;
        }
        if (q.matches("(?i)^[0-9+\\-*/().=\\s?？]+$")) {
            return true;
        }
        return q.length() <= 12 && !containsPlanningIntent(q);
    }

    private static boolean containsPlanningIntent(String text) {
        return text.indexOf("\u63d0\u9192") >= 0
                || text.indexOf("\u5f85\u529e") >= 0
                || text.indexOf("\u9489\u9009") >= 0
                || text.indexOf("\u5b89\u6392") >= 0
                || text.indexOf("\u8ba1\u5212") >= 0
                || text.indexOf("\u65e5\u7a0b") >= 0
                || (text.indexOf("\u5468") >= 0 && text.indexOf("\u671f") >= 0);
    }

    private String applyChatPlaceholders(String template, MapModel map, String userQuestion, AiChatSession session,
            NodeModel focusNode) {
        return applyChatPlaceholders(template, map, userQuestion, session, focusNode, null);
    }

    private String applyChatPlaceholders(String template, MapModel map, String userQuestion, AiChatSession session,
            NodeModel focusNode, AiPromptBuildProgress progress) {

        String safeTemplate = template != null ? template : "";

        String mapPath = resolveMapPath(map);

        String mapTitle = map != null && map.getTitle() != null ? map.getTitle() : "\u65e0";



        AiContextCollector.ContextBundle context = AiContextCollector.collect(map, userQuestion, progress,
                isLightweightQuestion(userQuestion));

        String mapContent = context.getCombinedMapContent();

        String referencedFiles = context.getReferencedFiles();

        if (referencedFiles == null || referencedFiles.trim().length() == 0) {

            referencedFiles = "\uff08\u65e0\u5173\u8054\u6587\u4ef6\u6216\u5df2\u8bfb\u53d6\u5bfc\u56fe\u672c\u8eab\uff09";

        }



        String chatHistory = AiChatHistoryFormatter.formatForContext(session, true);

        report(progress, 3, 6, "\u5339\u914d\u5173\u952e\u8bcd\u89c4\u5219...");
        String keywords = templateStore.getKeywordsText();
        String activeKeywordRules = templateStore.getActiveKeywordRules(userQuestion);
        if (activeKeywordRules == null || activeKeywordRules.trim().length() == 0) {
            activeKeywordRules = "\uff08\u672a\u5339\u914d\u5173\u952e\u8bcd\uff09";
        }

        String workspacePlans;
        if (isLightweightQuestion(userQuestion)) {
            report(progress, 4, 6, "\u7b80\u5355\u95ee\u9898\uff0c\u8df3\u8fc7\u5168\u5c40\u5b89\u6392...");
            workspacePlans = "\uff08\u7b80\u5355\u95ee\u9898\uff0c\u672a\u52a0\u8f7d\u5168\u5c40\u5b89\u6392\uff09";
        }
        else {
            workspacePlans = AiWorkspacePlanCollector.collectWorkspacePlans(progress, userQuestion);
        }

        String workspaceFileIndex;
        if (isLightweightQuestion(userQuestion)) {
            workspaceFileIndex = "\uff08\u7b80\u5355\u95ee\u9898\uff0c\u672a\u52a0\u8f7d\u5bfc\u56fe\u5e93\u6587\u4ef6\u7d22\u5f15\uff09";
        }
        else {
            workspaceFileIndex = AiWorkspaceFileIndexCollector.collectWorkspaceFileIndex(progress, userQuestion);
        }

        report(progress, 5, 6, "\u7ec4\u88c5\u63d0\u793a\u8bcd...");
        String question = userQuestion != null ? userQuestion : "";
        NodeModel resolvedFocus = focusNode != null ? focusNode : resolveCurrentSelectedNode();
        String selectedNodeContent = AiSelectedNodeExtractor.extract(resolvedFocus);

        String prompt = safeTemplate
                .replace("{{MAP_PATH}}", mapPath)
                .replace("{{MAP_TITLE}}", mapTitle)
                .replace("{{MAP_CONTENT}}", mapContent)
                .replace("{{REFERENCED_FILES}}", referencedFiles)
                .replace("{{SELECTED_NODE}}", selectedNodeContent)
                .replace("{{KEYWORDS}}", keywords)
                .replace("{{ACTIVE_KEYWORD_RULES}}", activeKeywordRules)
                .replace("{{WORKSPACE_PLANS}}", workspacePlans)
                .replace("{{WORKSPACE_FILE_INDEX}}", workspaceFileIndex)
                .replace("{{CHAT_HISTORY}}", chatHistory)
                .replace("{{USER_QUESTION}}", question);



        if (safeTemplate.indexOf("{{MAP_CONTENT}}") < 0) {

            prompt = prompt + "\n\n--- \u5f53\u524d\u601d\u7ef4\u5bfc\u56fe\u5185\u5bb9 ---\n" + mapContent;

        }

        if (safeTemplate.indexOf("{{REFERENCED_FILES}}") < 0 && context.getFilesIncluded() > 0) {

            prompt = prompt + "\n\n--- \u5173\u8054\u6587\u4ef6\u5185\u5bb9 ---\n" + referencedFiles;

        }

        if (safeTemplate.indexOf("{{KEYWORDS}}") < 0 && keywords.length() > 0

                && !"\uff08\u672a\u914d\u7f6e\u5173\u952e\u8bcd\uff09".equals(keywords)) {

            prompt = prompt + "\n\n--- \u5173\u952e\u8bcd\u53c2\u8003 ---\n" + keywords;

        }

        if (safeTemplate.indexOf("{{ACTIVE_KEYWORD_RULES}}") < 0
                && activeKeywordRules.length() > 0
                && !"\uff08\u672a\u5339\u914d\u5173\u952e\u8bcd\uff09".equals(activeKeywordRules)) {
            prompt = prompt + "\n\n--- \u5f53\u524d\u5339\u914d\u7684\u5173\u952e\u8bcd\u89c4\u5219 ---\n" + activeKeywordRules;
        }

        if (safeTemplate.indexOf("{{WORKSPACE_PLANS}}") < 0
                && workspacePlans != null
                && workspacePlans.trim().length() > 0) {
            prompt = prompt + "\n\n--- \u5168\u5c40\u5b89\u6392\u4e0e\u5173\u6ce8 ---\n" + workspacePlans;
        }

        if (safeTemplate.indexOf("{{WORKSPACE_FILE_INDEX}}") < 0
                && workspaceFileIndex != null
                && workspaceFileIndex.trim().length() > 0
                && !workspaceFileIndex.startsWith("\uff08\u7b80\u5355\u95ee\u9898")
                && !workspaceFileIndex.startsWith("\uff08\u5df2\u5173\u95ed")) {
            prompt = prompt + "\n\n--- \u5bfc\u56fe\u5e93\u6587\u4ef6\u7d22\u5f15\uff08\u538b\u7f29\u8def\u5f84\uff09 ---\n" + workspaceFileIndex;
        }

        if (safeTemplate.indexOf("{{CHAT_HISTORY}}") < 0 && session != null && !session.getMessages().isEmpty()) {
            prompt = prompt + "\n\n--- \u5386\u53f2\u5bf9\u8bdd ---\n" + chatHistory;
        }
        if (safeTemplate.indexOf("{{SELECTED_NODE}}") < 0 && resolvedFocus != null) {
            prompt = prompt + "\n\n--- \u5f53\u524d\u9009\u4e2d\u8282\u70b9 ---\n" + selectedNodeContent;
        }
        return prompt;
    }

    private static NodeModel resolveCurrentSelectedNode() {
        try {
            return Controller.getCurrentController().getSelection().getSelected();
        } catch (Exception e) {
            return null;
        }
    }



    public static String resolveMapPath(MapModel map) {

        if (map == null) {

            return "\uff08\u672a\u6253\u5f00\u601d\u7ef4\u5bfc\u56fe\uff09";

        }

        File file = map.getFile();

        if (file == null) {

            return "\uff08\u5f53\u524d\u601d\u7ef4\u5bfc\u56fe\u5c1a\u672a\u4fdd\u5b58\uff09";

        }

        return file.getAbsolutePath();

    }

}

