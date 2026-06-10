package org.docear.plugin.ai.log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.docear.plugin.ai.DocearAiConfig;
import org.docear.plugin.ai.ui.AiCopilotResponseParser;
import org.freeplane.core.util.LogUtils;

/**
 * 将 AI 交互记录追加写入 ai_logs/{导图名}/YYYY-MM.log。
 * 默认仅保存用户输入与 AI 回复；完整 prompt 需开启 ai.log_prompt。
 */
public class AiInteractionLogger {

    private static final String ENTRY_SEPARATOR = "\n---\n\n";

    private final File logDirectory;
    private final DocearAiConfig config;

    public AiInteractionLogger() {
        this(new File(new DocearAiConfig().getInteractionLogDirectory()), new DocearAiConfig());
    }

    public AiInteractionLogger(File logDirectory) {
        this(logDirectory, new DocearAiConfig());
    }

    AiInteractionLogger(File logDirectory, DocearAiConfig config) {
        this.logDirectory = logDirectory;
        this.config = config;
        AiInteractionLogMigrator.migrateIfNeeded(logDirectory);
    }

    public File getLogDirectory() {
        return logDirectory;
    }

    public File getAiLogsDirectory() {
        return new File(logDirectory, config.getAiLogsDirectoryName());
    }

    public void ensureLogDirectoryExists() {
        File aiLogsDir = getAiLogsDirectory();
        if (!aiLogsDir.exists()) {
            boolean created = aiLogsDir.mkdirs();
            if (created) {
                LogUtils.info("Created AI logs directory: " + aiLogsDir.getAbsolutePath());
            }
        }
    }

    public void log(AiInteractionRecord record) {
        if (record == null) {
            return;
        }
        ensureLogDirectoryExists();
        File logFile = resolveMonthlyLogFile(record);
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(logFile, true), "UTF-8");
            if (logFile.length() > 0) {
                writer.write(ENTRY_SEPARATOR);
            }
            writer.write(formatRecord(record));
            writer.flush();
            appendIndex(record, logFile);
            LogUtils.info("AI interaction appended: " + logFile.getAbsolutePath());
        } catch (Exception e) {
            LogUtils.severe("Failed to write AI interaction log: " + e.getMessage());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private File resolveMonthlyLogFile(AiInteractionRecord record) {
        String mapFolder = resolveMapFolderName(record);
        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM");
        String month = monthFormat.format(new Date(record.getTimestamp()));
        File mapDir = new File(getAiLogsDirectory(), mapFolder);
        if (!mapDir.exists()) {
            mapDir.mkdirs();
        }
        return new File(mapDir, month + ".log");
    }

    private String formatRecord(AiInteractionRecord record) {
        SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(displayFormat.format(new Date(record.getTimestamp())));
        sb.append(" | ").append(record.getType());
        sb.append(" | ").append(resolveMapDisplayName(record)).append(" ===\n");

        if (AiInteractionRecord.TYPE_CHAT.equals(record.getType())) {
            sb.append("[\u7528\u6237]\n").append(record.getUserInput()).append("\n\n");
            sb.append("[AI]\n").append(formatAssistantResponse(record.getResponse())).append('\n');
        } else {
            sb.append("[\u8f93\u5165]\n").append(record.getUserInput()).append("\n\n");
            sb.append("[\u8f93\u51fa]\n").append(record.getResponse()).append('\n');
        }

        if (config.isLogPromptEnabled()) {
            sb.append("\n[\u5b8c\u6574 Prompt]\n").append(record.getPromptSent()).append('\n');
        }
        return sb.toString();
    }

    private String formatAssistantResponse(String response) {
        if (response == null || response.trim().length() == 0) {
            return "";
        }
        AiCopilotResponseParser.ParsedResponse parsed = AiCopilotResponseParser.parse(response);
        if (parsed.getFinalAnswer().length() > 0) {
            return parsed.getFinalAnswer();
        }
        return response;
    }

    private void appendIndex(AiInteractionRecord record, File logFile) {
        OutputStreamWriter writer = null;
        try {
            File indexFile = new File(getAiLogsDirectory(), "index.tsv");
            writer = new OutputStreamWriter(new FileOutputStream(indexFile, true), "UTF-8");
            SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            writer.write(displayFormat.format(new Date(record.getTimestamp())));
            writer.write('\t');
            writer.write(record.getType());
            writer.write('\t');
            writer.write(relativePath(logFile));
            writer.write('\t');
            writer.write(record.getMapPath());
            writer.write('\t');
            writer.write(sanitizeIndexValue(record.getUserInput()));
            writer.write('\n');
            writer.flush();
        } catch (Exception e) {
            LogUtils.warn("Failed to append AI interaction index: " + e.getMessage());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private String relativePath(File logFile) {
        String base = getAiLogsDirectory().getAbsolutePath();
        String absolute = logFile.getAbsolutePath();
        if (absolute.startsWith(base)) {
            String relative = absolute.substring(base.length());
            if (relative.startsWith(File.separator) || relative.startsWith("/")) {
                relative = relative.substring(1);
            }
            return relative.replace('\\', '/');
        }
        return logFile.getName();
    }

    private String resolveMapFolderName(AiInteractionRecord record) {
        String title = resolveMapDisplayName(record);
        if (title.length() > 0) {
            return sanitizeFolderName(title);
        }
        return "unknown_map";
    }

    private String resolveMapDisplayName(AiInteractionRecord record) {
        String title = record.getMapTitle();
        if (title != null && title.trim().length() > 0 && !"\u65e0".equals(title.trim())) {
            return title.trim();
        }
        String path = record.getMapPath();
        if (path != null && path.trim().length() > 0) {
            File file = new File(path.trim());
            String name = file.getName();
            if (name.endsWith(".mm")) {
                name = name.substring(0, name.length() - 3);
            }
            if (name.trim().length() > 0) {
                return name.trim();
            }
        }
        return "";
    }

    private String sanitizeFolderName(String value) {
        String safe = value.replace("\\", "_").replace("/", "_").replace(":", "_")
                .replace("*", "_").replace("?", "_").replace("\"", "_")
                .replace("<", "_").replace(">", "_").replace("|", "_");
        safe = safe.trim();
        if (safe.length() == 0) {
            return "unknown_map";
        }
        if (safe.length() > 80) {
            safe = safe.substring(0, 80);
        }
        return safe;
    }

    private String sanitizeIndexValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }
}
