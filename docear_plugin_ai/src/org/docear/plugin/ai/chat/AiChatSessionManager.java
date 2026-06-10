package org.docear.plugin.ai.chat;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.docear.plugin.ai.DocearAiConfig;
import org.freeplane.features.map.MapModel;

/**
 * 管理每个思维导图对应的聊天会话。
 * 完整历史保存在交互日志目录；.mm 文件仅保留最近若干轮。
 */
public class AiChatSessionManager {

    private final AiChatSessionStore store;
    private final Map<String, AiChatSession> cache = new HashMap<String, AiChatSession>();

    public AiChatSessionManager() {
        this(new AiChatSessionStore());
    }

    public AiChatSessionManager(AiChatSessionStore store) {
        this.store = store;
        this.store.ensureDirectoryExists();
    }

    public AiChatSessionStore getStore() {
        return store;
    }

    public AiChatSession getOrCreateSession(MapModel map) {
        String mapKey = resolveMapKey(map);
        if (cache.containsKey(mapKey)) {
            return cache.get(mapKey);
        }

        AiChatSession fileSession = store.load(mapKey);
        if (!fileSession.getMessages().isEmpty()) {
            cache.put(mapKey, fileSession);
            syncToMapExtension(map, fileSession);
            return fileSession;
        }

        AiChatHistoryExtension extension = AiChatHistoryExtension.get(map);
        if (extension != null && extension.getSession() != null
                && !extension.getSession().getMessages().isEmpty()) {
            cache.put(mapKey, extension.getSession());
            store.save(extension.getSession());
            return extension.getSession();
        }

        cache.put(mapKey, fileSession);
        syncToMapExtension(map, fileSession);
        return fileSession;
    }

    public void saveSession(MapModel map, AiChatSession session) {
        if (session == null) {
            return;
        }
        String mapKey = resolveMapKey(map);
        cache.put(mapKey, session);
        store.save(session);
        syncToMapExtension(map, session);
    }

    public void clearSession(MapModel map) {
        String mapKey = resolveMapKey(map);
        AiChatSession session = cache.containsKey(mapKey) ? cache.get(mapKey) : store.load(mapKey);
        if (session == null) {
            session = new AiChatSession(mapKey);
        }
        session.clear();
        cache.put(mapKey, session);
        store.save(session);
        syncToMapExtension(map, session);
    }

    private void syncToMapExtension(MapModel map, AiChatSession fullSession) {
        if (map == null || fullSession == null) {
            return;
        }
        int maxRounds = new DocearAiConfig().getMaxMmChatRounds();
        AiChatSession mmSession = fullSession.trimToLastRounds(maxRounds);
        AiChatHistoryExtension extension = AiChatHistoryExtension.getOrCreate(map);
        extension.setSession(mmSession);
    }

    public static String resolveMapKey(MapModel map) {
        if (map == null) {
            return "no_map";
        }
        File file = map.getFile();
        if (file != null) {
            return file.getAbsolutePath();
        }
        String title = map.getTitle();
        if (title != null && title.trim().length() > 0) {
            return "unsaved_" + title.trim();
        }
        return "unsaved_" + System.identityHashCode(map);
    }
}
