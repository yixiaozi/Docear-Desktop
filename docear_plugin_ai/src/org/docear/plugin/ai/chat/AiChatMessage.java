package org.docear.plugin.ai.chat;

/**
 * 单条聊天消息。
 * 兼容 Java 1.6。
 */
public class AiChatMessage {

    public enum Role {
        USER,
        ASSISTANT
    }

    private final Role role;
    private final String content;
    private final long timestamp;

    public AiChatMessage(Role role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    public AiChatMessage(Role role, String content, long timestamp) {
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
    }

    public Role getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
