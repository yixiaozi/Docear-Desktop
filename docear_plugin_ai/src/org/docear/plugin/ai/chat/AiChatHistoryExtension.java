package org.docear.plugin.ai.chat;

import org.freeplane.core.extension.IExtension;

/**
 * 挂载到 MapModel 上的聊天历史 Extension。
 * 每个思维导图（MapModel）持有一个独立的聊天会话。
 */
public class AiChatHistoryExtension implements IExtension {

    private AiChatSession session;

    public AiChatHistoryExtension(String mapId) {
        this.session = new AiChatSession(mapId);
    }

    public AiChatSession getSession() {
        return session;
    }

    public void setSession(AiChatSession session) {
        this.session = session;
    }
}
