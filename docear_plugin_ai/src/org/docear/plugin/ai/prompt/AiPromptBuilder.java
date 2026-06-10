package org.docear.plugin.ai.prompt;



import java.io.File;



import org.docear.plugin.ai.chat.AiChatHistoryFormatter;

import org.docear.plugin.ai.chat.AiChatSession;

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

    public String buildRawChatPrompt(String userQuestion, MapModel map, AiChatSession session) {
        return buildRawChatPrompt(userQuestion, map, session, null);
    }

    public String buildRawChatPrompt(String userQuestion, MapModel map, AiChatSession session, NodeModel focusNode) {
        templateStore.reloadIfChanged();
        String template = templateStore.getChatTemplate();
        return applyChatPlaceholders(template, map, userQuestion, session, focusNode);
    }

    private String applyChatPlaceholders(String template, MapModel map, String userQuestion, AiChatSession session,
            NodeModel focusNode) {

        String safeTemplate = template != null ? template : "";

        String mapPath = resolveMapPath(map);

        String mapTitle = map != null && map.getTitle() != null ? map.getTitle() : "\u65e0";



        AiContextCollector.ContextBundle context = AiContextCollector.collect(map, userQuestion);

        String mapContent = context.getCombinedMapContent();

        String referencedFiles = context.getReferencedFiles();

        if (referencedFiles == null || referencedFiles.trim().length() == 0) {

            referencedFiles = "\uff08\u65e0\u5173\u8054\u6587\u4ef6\u6216\u5df2\u8bfb\u53d6\u5bfc\u56fe\u672c\u8eab\uff09";

        }



        String chatHistory = AiChatHistoryFormatter.formatForContext(session, true);

        String keywords = templateStore.getKeywordsText();
        String activeKeywordRules = templateStore.getActiveKeywordRules(userQuestion);
        if (activeKeywordRules == null || activeKeywordRules.trim().length() == 0) {
            activeKeywordRules = "\uff08\u672a\u5339\u914d\u5173\u952e\u8bcd\uff09";
        }

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

