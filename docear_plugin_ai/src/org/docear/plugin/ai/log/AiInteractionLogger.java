package org.docear.plugin.ai.log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.docear.plugin.ai.DocearAiConfig;
import org.freeplane.core.util.LogUtils;

/**
 * 将发给 AI 的提示词及回复记录到指定目录。
 */
public class AiInteractionLogger {

    private final File logDirectory;

    public AiInteractionLogger() {
        this(new File(new DocearAiConfig().getInteractionLogDirectory()));
    }

    public AiInteractionLogger(File logDirectory) {
        this.logDirectory = logDirectory;
    }

    public File getLogDirectory() {
        return logDirectory;
    }

    public void ensureLogDirectoryExists() {
        if (!logDirectory.exists()) {
            boolean created = logDirectory.mkdirs();
            if (created) {
                LogUtils.info("Created AI interaction log directory: " + logDirectory.getAbsolutePath());
            }
        }
    }

    public void log(AiInteractionRecord record) {
        if (record == null) {
            return;
        }
        ensureLogDirectoryExists();
        File logFile = createLogFile(record);
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(logFile), "UTF-8");
            writer.write(formatRecord(record));
            writer.flush();
            appendIndex(record, logFile);
            LogUtils.info("AI interaction logged: " + logFile.getAbsolutePath());
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

    private File createLogFile(AiInteractionRecord record) {
        SimpleDateFormat nameFormat = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");
        String fileName = nameFormat.format(new Date(record.getTimestamp())) + "_" + record.getType() + ".txt";
        return new File(logDirectory, fileName);
    }

    private String formatRecord(AiInteractionRecord record) {
        SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        sb.append("=== AI Interaction ===\n");
        sb.append("Time: ").append(displayFormat.format(new Date(record.getTimestamp()))).append('\n');
        sb.append("Type: ").append(record.getType()).append('\n');
        sb.append("Map Path: ").append(record.getMapPath()).append('\n');
        sb.append("Map Title: ").append(record.getMapTitle()).append('\n');
        sb.append("User Input:\n").append(record.getUserInput()).append("\n\n");
        sb.append("--- Prompt Sent to AI ---\n");
        sb.append(record.getPromptSent()).append("\n\n");
        sb.append("--- AI Response ---\n");
        sb.append(record.getResponse()).append('\n');
        return sb.toString();
    }

    private void appendIndex(AiInteractionRecord record, File logFile) {
        OutputStreamWriter writer = null;
        try {
            File indexFile = new File(logDirectory, "ai-interactions-index.txt");
            writer = new OutputStreamWriter(new FileOutputStream(indexFile, true), "UTF-8");
            SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            writer.write(displayFormat.format(new Date(record.getTimestamp())));
            writer.write('\t');
            writer.write(record.getType());
            writer.write('\t');
            writer.write(logFile.getName());
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

    private String sanitizeIndexValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }
}
