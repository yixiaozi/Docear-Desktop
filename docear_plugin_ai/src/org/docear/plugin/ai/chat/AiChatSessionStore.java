package org.docear.plugin.ai.chat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

import org.docear.plugin.ai.DocearAiConfig;
import org.freeplane.core.util.LogUtils;

/**
 * 将每个思维导图的聊天会话持久化到交互记录目录下的 chat_sessions 子目录。
 */
public class AiChatSessionStore {

    private static final String SESSION_DIR_NAME = "chat_sessions";

    private final File sessionDirectory;

    public AiChatSessionStore() {
        this(new File(new DocearAiConfig().getInteractionLogDirectory(), SESSION_DIR_NAME));
    }

    public AiChatSessionStore(File sessionDirectory) {
        this.sessionDirectory = sessionDirectory;
    }

    public File getSessionDirectory() {
        return sessionDirectory;
    }

    public void ensureDirectoryExists() {
        if (!sessionDirectory.exists()) {
            sessionDirectory.mkdirs();
        }
    }

    public AiChatSession load(String mapKey) {
        ensureDirectoryExists();
        File file = getSessionFile(mapKey);
        if (!file.exists()) {
            return new AiChatSession(mapKey);
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            AiChatSession session = new AiChatSession(mapKey);
            String line;
            String role = null;
            long timestamp = 0L;
            StringBuilder content = new StringBuilder();
            boolean inContent = false;

            while ((line = reader.readLine()) != null) {
                if ("---MESSAGE---".equals(line)) {
                    if (role != null) {
                        session.addMessage(new AiChatMessage(parseRole(role), content.toString(), timestamp));
                        role = null;
                        content.setLength(0);
                        inContent = false;
                    }
                    continue;
                }
                if (line.startsWith("role=")) {
                    role = line.substring("role=".length());
                    inContent = false;
                    continue;
                }
                if (line.startsWith("time=")) {
                    timestamp = Long.parseLong(line.substring("time=".length()));
                    continue;
                }
                if ("content=".equals(line)) {
                    inContent = true;
                    continue;
                }
                if (inContent) {
                    if (content.length() > 0) {
                        content.append('\n');
                    }
                    content.append(unescape(line));
                }
            }
            if (role != null) {
                session.addMessage(new AiChatMessage(parseRole(role), content.toString(), timestamp));
            }
            return session;
        } catch (Exception e) {
            LogUtils.warn("Failed to load chat session: " + file + ", " + e.getMessage());
            return new AiChatSession(mapKey);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void save(AiChatSession session) {
        if (session == null) {
            return;
        }
        ensureDirectoryExists();
        File file = getSessionFile(session.getMapId());
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
            writer.write("# Docear AI Chat Session\n");
            writer.write("mapKey=" + session.getMapId() + "\n");
            writer.write("lastUpdated=" + session.getLastUpdated() + "\n");

            List<AiChatMessage> messages = session.getMessages();
            for (int i = 0; i < messages.size(); i++) {
                AiChatMessage message = messages.get(i);
                writer.write("---MESSAGE---\n");
                writer.write("role=" + message.getRole().name() + "\n");
                writer.write("time=" + message.getTimestamp() + "\n");
                writer.write("content=\n");
                writer.write(escape(message.getContent()));
                writer.write("\n");
            }
            writer.flush();
        } catch (Exception e) {
            LogUtils.severe("Failed to save chat session: " + e.getMessage());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void delete(String mapKey) {
        File file = getSessionFile(mapKey);
        if (file.exists()) {
            file.delete();
        }
    }

    private File getSessionFile(String mapKey) {
        return new File(sessionDirectory, sanitizeFileName(mapKey) + ".chat");
    }

    private String sanitizeFileName(String mapKey) {
        if (mapKey == null) {
            return "unknown";
        }
        String safe = mapKey.replace("\\", "_").replace("/", "_").replace(":", "_");
        if (safe.length() > 180) {
            safe = String.valueOf(safe.hashCode()) + "_" + safe.substring(safe.length() - 40);
        }
        return safe;
    }

    private AiChatMessage.Role parseRole(String role) {
        if ("ASSISTANT".equalsIgnoreCase(role)) {
            return AiChatMessage.Role.ASSISTANT;
        }
        return AiChatMessage.Role.USER;
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\");
    }

    private String unescape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\\\", "\\");
    }
}
