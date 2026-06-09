package org.docear.plugin.ai.log;

/**
 * 一次 AI 交互记录。
 */
public class AiInteractionRecord {

    public static final String TYPE_CHAT = "chat";
    public static final String TYPE_GENERATE_SUBNODES = "generate_subnodes";

    private final String type;
    private final String userInput;
    private final String promptSent;
    private final String response;
    private final String mapPath;
    private final String mapTitle;
    private final long timestamp;

    public AiInteractionRecord(String type, String userInput, String promptSent, String response,
            String mapPath, String mapTitle, long timestamp) {
        this.type = type;
        this.userInput = userInput != null ? userInput : "";
        this.promptSent = promptSent != null ? promptSent : "";
        this.response = response != null ? response : "";
        this.mapPath = mapPath != null ? mapPath : "";
        this.mapTitle = mapTitle != null ? mapTitle : "";
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public String getUserInput() {
        return userInput;
    }

    public String getPromptSent() {
        return promptSent;
    }

    public String getResponse() {
        return response;
    }

    public String getMapPath() {
        return mapPath;
    }

    public String getMapTitle() {
        return mapTitle;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
