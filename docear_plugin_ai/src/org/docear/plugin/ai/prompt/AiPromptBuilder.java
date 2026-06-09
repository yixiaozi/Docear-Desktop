package org.docear.plugin.ai.prompt;

import java.io.File;

import org.freeplane.features.map.MapModel;

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
        templateStore.ensureTemplateFileExists();
        String template = templateStore.getChatTemplate();
        return applyPlaceholders(template, map, userQuestion);
    }

    public String buildSubNodesPrompt(String topic, MapModel map, int count) {
        templateStore.ensureTemplateFileExists();
        String template = templateStore.getSubNodesTemplate();
        String prompt = applyPlaceholders(template, map, topic);
        return prompt + "\n\n请生成 " + count + " 个子主题（仅返回标题列表，每行一个，不要解释）。";
    }

    private String applyPlaceholders(String template, MapModel map, String userQuestion) {
        String safeTemplate = template != null ? template : "";
        String mapPath = resolveMapPath(map);
        String mapTitle = map != null && map.getTitle() != null ? map.getTitle() : "\u65e0";
        String question = userQuestion != null ? userQuestion : "";

        return safeTemplate
                .replace("{{MAP_PATH}}", mapPath)
                .replace("{{MAP_TITLE}}", mapTitle)
                .replace("{{USER_QUESTION}}", question);
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
