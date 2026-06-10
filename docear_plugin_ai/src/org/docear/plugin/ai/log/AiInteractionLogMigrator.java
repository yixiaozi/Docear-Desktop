package org.docear.plugin.ai.log;

import java.io.File;
import java.util.regex.Pattern;

import org.docear.plugin.ai.DocearAiConfig;
import org.freeplane.core.util.LogUtils;

/**
 * 将旧版散落的单次交互 txt 日志迁移到 ai_logs/archive/。
 */
public final class AiInteractionLogMigrator {

    private static final Pattern LEGACY_LOG_FILE = Pattern.compile(
            "\\d{8}_\\d{6}_\\d{3}_(chat|generate_subnodes)\\.txt");
    private static final String MIGRATION_MARKER = ".legacy_migrated";

    private AiInteractionLogMigrator() {
    }

    public static void migrateIfNeeded(File logDirectory) {
        if (logDirectory == null || !logDirectory.isDirectory()) {
            return;
        }
        File aiLogsDir = new File(logDirectory, new DocearAiConfig().getAiLogsDirectoryName());
        File marker = new File(aiLogsDir, MIGRATION_MARKER);
        if (marker.exists()) {
            return;
        }

        File archiveDir = new File(aiLogsDir, "archive");
        if (!archiveDir.exists()) {
            archiveDir.mkdirs();
        }

        int moved = 0;
        File[] files = logDirectory.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (!file.isFile()) {
                    continue;
                }
                String name = file.getName();
                if (LEGACY_LOG_FILE.matcher(name).matches()
                        || "ai-interactions-index.txt".equals(name)) {
                    File target = new File(archiveDir, name);
                    if (target.exists()) {
                        target = uniqueTarget(archiveDir, name);
                    }
                    if (file.renameTo(target)) {
                        moved++;
                    } else {
                        LogUtils.warn("Failed to migrate legacy AI log: " + file.getAbsolutePath());
                    }
                }
            }
        }

        try {
            marker.createNewFile();
        } catch (Exception e) {
            LogUtils.warn("Failed to write AI log migration marker: " + e.getMessage());
        }
        if (moved > 0) {
            LogUtils.info("Migrated " + moved + " legacy AI log file(s) to "
                    + archiveDir.getAbsolutePath());
        }
    }

    private static File uniqueTarget(File archiveDir, String name) {
        int suffix = 1;
        while (true) {
            File candidate = new File(archiveDir, name + "." + suffix);
            if (!candidate.exists()) {
                return candidate;
            }
            suffix++;
        }
    }
}
