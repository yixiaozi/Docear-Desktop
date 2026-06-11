package org.docear.plugin.ai.usage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.docear.plugin.ai.DocearAiConfig;
import org.freeplane.core.util.LogUtils;

/**
 * Copilot CLI 调用次数计数器。按日/月统计本地调用次数，
 * 优先从对话历史日志中读取统计数据（更准确），同时维护独立的计数器文件作为备份。
 */
public class AiUsageCounter {

    public static final String TYPE_CHAT = "chat";
    public static final String TYPE_SUBNODES = "subnodes";

    private static final String COUNTER_DIR_NAME = "usage";
    private static final String COUNTER_FILE_NAME = "counter.tsv";
    private static final String WARNING_MARK_FILE = "warning_shown";
    private static final String ENTRY_SEPARATOR = "\n---\n\n";

    private final File counterFile;
    private final File warningMarkFile;
    private final File logsDirectory;
    private final DocearAiConfig config;

    private final Map<String, Integer> dailyCounts = new LinkedHashMap<String, Integer>();
    private final Map<String, Integer> monthlyCounts = new LinkedHashMap<String, Integer>();
    private long lastLoaded;

    public AiUsageCounter() {
        this(new DocearAiConfig());
    }

    AiUsageCounter(DocearAiConfig config) {
        this.config = config;
        File counterDir = new File(config.getAiHomeDirectory(), COUNTER_DIR_NAME);
        if (!counterDir.exists()) {
            counterDir.mkdirs();
        }
        this.counterFile = new File(counterDir, COUNTER_FILE_NAME);
        this.warningMarkFile = new File(counterDir, WARNING_MARK_FILE);
        this.logsDirectory = new File(new File(config.getAiHomeDirectory()), config.getAiLogsDirectoryName());
        loadIfChanged();
    }

    /**
     * 记录一次调用（线程安全）。
     */
    public synchronized void recordInvocation(String type) {
        loadIfChanged();
        String todayKey = dayKey(new Date());
        String monthKey = monthKey(new Date());
        increment(dailyCounts, todayKey, 1);
        increment(monthlyCounts, monthKey, 1);
        save();
    }

    /**
     * 获得当天调用次数（从对话历史日志中统计）。
     */
    public synchronized int getTodayCount() {
        int count = countFromLogs(dayKey(new Date()));
        if (count > 0) {
            return count;
        }
        loadIfChanged();
        Integer stored = dailyCounts.get(dayKey(new Date()));
        return stored != null ? stored.intValue() : 0;
    }

    /**
     * 获得当月调用次数（从对话历史日志中统计，更准确）。
     */
    public synchronized int getThisMonthCount() {
        int count = countFromLogs(monthKey(new Date()));
        if (count > 0) {
            return count;
        }
        loadIfChanged();
        Integer stored = monthlyCounts.get(monthKey(new Date()));
        return stored != null ? stored.intValue() : 0;
    }

    /**
     * 从对话历史日志中统计调用次数。
     * @param periodKey "YYYY-MM-DD" 格式表示当天，"YYYY-MM" 格式表示当月
     */
    private int countFromLogs(String periodKey) {
        if (!logsDirectory.exists() || !logsDirectory.isDirectory()) {
            return 0;
        }
        int count = 0;
        String monthKey = periodKey.length() == 10 ? periodKey.substring(0, 7) : periodKey;
        String dayKey = periodKey.length() == 10 ? periodKey : null;
        File[] allFiles = logsDirectory.listFiles();
        if (allFiles == null) {
            return 0;
        }
        for (File mapDir : allFiles) {
            if (!mapDir.isDirectory()) {
                continue;
            }
            File logFile = new File(mapDir, monthKey + ".log");
            if (!logFile.exists()) {
                continue;
            }
            count += countEntriesInLog(logFile, dayKey);
        }
        return count;
    }

    /**
     * 统计单个日志文件中的条目数。
     * @param logFile 日志文件
     * @param dayFilter 日期过滤器（null 表示统计整月）
     */
    private int countEntriesInLog(File logFile, String dayFilter) {
        if (!logFile.exists()) {
            return 0;
        }
        int count = 0;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(logFile));
            String line;
            boolean inEntry = false;
            String currentDate = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("===")) {
                    int dateEnd = line.indexOf(" | ");
                    if (dateEnd > 0) {
                        currentDate = line.substring(4, dateEnd).trim();
                        if (currentDate.length() >= 10) {
                            currentDate = currentDate.substring(0, 10);
                        }
                    }
                    inEntry = true;
                } else if (line.startsWith("---") && inEntry) {
                    if (dayFilter == null || dayFilter.equals(currentDate)) {
                        count++;
                    }
                    inEntry = false;
                }
            }
            if (inEntry && (dayFilter == null || dayFilter.equals(currentDate))) {
                count++;
            }
        } catch (Exception e) {
            LogUtils.warn("Failed to read log file for usage counting: " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
        return count;
    }

    /**
     * 返回当前月份配额上限（来自配置项 ai.monthly_quota）。
     */
    public int getMonthlyQuota() {
        return config.getMonthlyQuota();
    }

    /**
     * 当前月用量百分比（0.0 ~ 1.0+，可能因为上限保守而略大于 1）。
     */
    public double getMonthlyUsageRatio() {
        int quota = getMonthlyQuota();
        if (quota <= 0) {
            return 0d;
        }
        return (double) getThisMonthCount() / (double) quota;
    }

    /**
     * 判断是否接近上限（>= 80%）。
     */
    public boolean isNearQuota() {
        return getMonthlyUsageRatio() >= 0.8d;
    }

    /**
     * 判断是否已达到/超过上限。
     */
    public boolean isOverQuota() {
        int quota = getMonthlyQuota();
        return quota > 0 && getThisMonthCount() >= quota;
    }

    /**
     * 判断是否需要弹出一次警告（超限时调用，避免每次聊天都提示）。
     */
    public synchronized boolean consumeWarningIfDue() {
        if (!isNearQuota() && !isOverQuota()) {
            return false;
        }
        if (warningMarkFile.exists()) {
            long age = System.currentTimeMillis() - warningMarkFile.lastModified();
            if (age < config.getUsageWarningCooldownMs()) {
                return false;
            }
        }
        try {
            new FileOutputStream(warningMarkFile).close();
        } catch (Exception ignored) {
        }
        return true;
    }

    /**
     * 重新加载（如文件在外部被修改过）。
     */
    private void loadIfChanged() {
        if (!counterFile.exists()) {
            return;
        }
        long modified = counterFile.lastModified();
        if (modified == lastLoaded) {
            return;
        }
        BufferedReader reader = null;
        try {
            dailyCounts.clear();
            monthlyCounts.clear();
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(counterFile), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0 || line.startsWith("#")) {
                    continue;
                }
                String[] parts = splitTab(line);
                if (parts.length < 2) {
                    continue;
                }
                String bucket = parts[0].trim();
                int count;
                try {
                    count = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    continue;
                }
                if (bucket.length() == 10) {
                    dailyCounts.put(bucket, Integer.valueOf(count));
                } else if (bucket.length() == 7) {
                    monthlyCounts.put(bucket, Integer.valueOf(count));
                }
            }
            lastLoaded = counterFile.lastModified();
        } catch (Exception e) {
            LogUtils.warn("Failed to load AI usage counter: " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void save() {
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(counterFile), "UTF-8");
            writer.write("# AI usage counter. Lines: date-bucket<TAB>count\n");
            writer.write("# Daily (YYYY-MM-DD)\n");
            for (Map.Entry<String, Integer> entry : dailyCounts.entrySet()) {
                writer.write(entry.getKey());
                writer.write('\t');
                writer.write(String.valueOf(entry.getValue()));
                writer.write('\n');
            }
            writer.write("# Monthly (YYYY-MM)\n");
            for (Map.Entry<String, Integer> entry : monthlyCounts.entrySet()) {
                writer.write(entry.getKey());
                writer.write('\t');
                writer.write(String.valueOf(entry.getValue()));
                writer.write('\n');
            }
            writer.flush();
            lastLoaded = counterFile.lastModified();
        } catch (Exception e) {
            LogUtils.warn("Failed to save AI usage counter: " + e.getMessage());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void increment(Map<String, Integer> map, String key, int delta) {
        Integer current = map.get(key);
        int value = current != null ? current.intValue() : 0;
        map.put(key, Integer.valueOf(value + delta));
    }

    private static String dayKey(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private static String monthKey(Date date) {
        return new SimpleDateFormat("yyyy-MM").format(date);
    }

    private static String[] splitTab(String line) {
        int idx = line.indexOf('\t');
        if (idx < 0) {
            return new String[] { line };
        }
        return new String[] { line.substring(0, idx), line.substring(idx + 1) };
    }

    /**
     * 生成一行可读的用量摘要，用于 UI 展示。
     */
    public String formatSummary() {
        int monthly = getThisMonthCount();
        int quota = getMonthlyQuota();
        int daily = getTodayCount();
        StringBuilder sb = new StringBuilder();
        sb.append("\u672c\u6708 ").append(monthly);
        if (quota > 0) {
            sb.append(" / ").append(quota);
            int percent = (int) Math.round(getMonthlyUsageRatio() * 100);
            sb.append(" (").append(percent).append("%)");
        }
        sb.append("  |  \u4eca\u65e5 ").append(daily);
        if (isOverQuota()) {
            sb.append("  |  \u26a0 \u914d\u989d\u5df2\u7528\u5b8c");
        } else if (isNearQuota()) {
            sb.append("  |  \u26a0 \u63a5\u8fd1\u914d\u989d");
        }
        return sb.toString();
    }

    public File getCounterFile() {
        return counterFile;
    }
}
