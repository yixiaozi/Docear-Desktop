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

    /**
     * 返回仅包含最近 maxRounds 轮对话的副本（一轮 = 一次用户提问及其后续回复）。
     */
    public AiChatSession trimToLastRounds(int maxRounds) {
        AiChatSession trimmed = new AiChatSession(mapId);
        if (maxRounds <= 0 || messages.isEmpty()) {
            return trimmed;
        }

        int rounds = 0;
        int startIndex = 0;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).getRole() == AiChatMessage.Role.USER) {
                rounds++;
                if (rounds >= maxRounds) {
                    startIndex = i;
                    break;
                }
            }
        }

        for (int i = startIndex; i < messages.size(); i++) {
            AiChatMessage message = messages.get(i);
            trimmed.messages.add(new AiChatMessage(message.getRole(), message.getContent(), message.getTimestamp()));
        }
        trimmed.lastUpdated = this.lastUpdated;
        return trimmed;
    }
}
