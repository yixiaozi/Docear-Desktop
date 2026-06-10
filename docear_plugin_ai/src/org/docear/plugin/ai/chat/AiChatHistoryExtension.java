package org.docear.plugin.ai.chat;

import org.freeplane.core.extension.IExtension;
import org.freeplane.features.map.MapModel;

/**
 * 挂载到 MapModel 上的聊天历史 Extension。
 * 每个思维导图（MapModel）持有一个独立的聊天会话，随 .mm 文件保存。
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

    public static AiChatHistoryExtension getOrCreate(MapModel map) {
        if (map == null) {
            return null;
        }
        AiChatHistoryExtension extension = map.getExtension(AiChatHistoryExtension.class);
        if (extension == null) {
            extension = new AiChatHistoryExtension(AiChatSessionManager.resolveMapKey(map));
            map.addExtension(AiChatHistoryExtension.class, extension);
        }
        return extension;
    }

    public static AiChatHistoryExtension get(MapModel map) {
        if (map == null) {
            return null;
        }
        return map.getExtension(AiChatHistoryExtension.class);
    }
}
