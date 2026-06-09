package org.docear.plugin.ai.chat;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个思维导图的聊天会话。
 * 每个 .mm 文件对应一个独立的聊天历史。
 */
public class AiChatSession {

    private final String mapId;
    private final List<AiChatMessage> messages;
    private long lastUpdated;

    public AiChatSession(String mapId) {
        this.mapId = mapId;
        this.messages = new ArrayList<AiChatMessage>();
        this.lastUpdated = System.currentTimeMillis();
    }

    public String getMapId() {
        return mapId;
    }

    public List<AiChatMessage> getMessages() {
        return messages;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void addMessage(AiChatMessage message) {
        messages.add(message);
        this.lastUpdated = System.currentTimeMillis();
    }

    public void clear() {
        messages.clear();
        this.lastUpdated = System.currentTimeMillis();
    }
}
