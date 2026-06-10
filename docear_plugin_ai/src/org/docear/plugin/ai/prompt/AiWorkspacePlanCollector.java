package org.docear.plugin.ai.prompt;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.docear.plugin.ai.DocearAiConfig;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.MindMapWorkspaceContextScanner;
import org.freeplane.core.util.MindMapWorkspaceContextScanner.ReminderItem;
import org.freeplane.core.util.MindMapWorkspaceContextScanner.TodoItem;
import org.freeplane.core.util.WorkspaceSideTabSnapshot;
import org.freeplane.core.util.WorkspaceSideTabSnapshotRegistry;

/**
 * 从侧栏 Tab 内存快照导出「全局安排与关注」摘要；仅在 Tab 尚未加载时才回退磁盘扫描。
 */
public final class AiWorkspacePlanCollector {

    private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
    private static final String[] QUESTION_STOP_WORDS = new String[] {
            "\u6211", "\u4f60", "\u6709", "\u4ec0\u4e48", "\u5417", "\u5462", "\u7684", "\u4e86",
            "\u5728", "\u662f", "\u6700\u8fd1", "\u8fd8", "\u6ca1", "\u6ca1\u6709", "\u54ea\u4e9b",
            "\u5982\u4f55", "\u600e\u4e48", "\u8bf7", "\u95ee"
    };

    private static final Object CACHE_LOCK = new Object();
    private static String cachedPlans;
    private static long cachedPlansAt;
    private static long cachedRegistryUpdatedAt = -1L;
    private static String cachedQuestionKey = "";

    private AiWorkspacePlanCollector() {
    }

    public static String collectWorkspacePlans() {
        return collectWorkspacePlans(new DocearAiConfig(), null);
    }

    public static String collectWorkspacePlans(AiPromptBuildProgress progress) {
        return collectWorkspacePlans(new DocearAiConfig(), progress, null);
    }

    public static String collectWorkspacePlans(AiPromptBuildProgress progress, String userQuestion) {
        return collectWorkspacePlans(new DocearAiConfig(), progress, userQuestion);
    }

    static String collectWorkspacePlans(DocearAiConfig config) {
        return collectWorkspacePlans(config, null, null);
    }

    static String collectWorkspacePlans(DocearAiConfig config, AiPromptBuildProgress progress) {
        return collectWorkspacePlans(config, progress, null);
    }

    static String collectWorkspacePlans(DocearAiConfig config, AiPromptBuildProgress progress, String userQuestion) {
        long started = System.currentTimeMillis();
        if (!config.isWorkspacePlansEnabled()) {
            return "\uff08\u5df2\u5173\u95ed\u5168\u5c40\u5b89\u6392\u4e0a\u4e0b\u6587\uff09";
        }
        int maxChars = config.getMaxWorkspacePlanChars();
        if (maxChars <= 0) {
            return "\uff08\u5df2\u7981\u7528\u5de5\u4f5c\u533a\u5b89\u6392\u4e0a\u4e0b\u6587\uff09";
        }
        int maxItems = config.getMaxWorkspacePlanItemsPerSection();
        long registryUpdatedAt = WorkspaceSideTabSnapshotRegistry.getLastUpdatedAt();
        String questionKey = userQuestion != null ? userQuestion.trim() : "";

        String cached = getCachedPlans(config, registryUpdatedAt, questionKey);
        if (cached != null) {
            report(progress, 4, 6, "\u4f7f\u7528\u7f13\u5b58\u7684\u5168\u5c40\u5b89\u6392\u6458\u8981");
            return cached;
        }

        WorkspaceSideTabSnapshot snapshot = WorkspaceSideTabSnapshotRegistry.getSnapshot();
        boolean usedTabSnapshot = snapshot.hasAnyItems() || registryUpdatedAt > 0L;

        StringBuilder sb = new StringBuilder();
        try {
            if (usedTabSnapshot) {
                report(progress, 4, 6, "\u6c47\u603b\u5168\u5c40\u5b89\u6392\uff1a\u63d0\u9192\u4e0e\u5f85\u529e\uff08\u6807\u7b7e\u9875\u7f13\u5b58\uff09...");
                appendReminderSection(sb, "\u5168\u90e8\u63d0\u9192",
                        filterReminderEntries(snapshot.getOneTimeReminders(), questionKey), maxItems);
                appendReminderSection(sb, "\u5468\u671f\u63d0\u9192",
                        filterReminderEntries(snapshot.getRecurringReminders(), questionKey), maxItems);
                appendTodoSection(sb, filterTodoEntries(snapshot.getTodos(), questionKey), maxItems);
                report(progress, 4, 6, "\u6c47\u603b\u5168\u5c40\u5b89\u6392\uff1a\u9489\u9009\u8282\u70b9\uff08\u6807\u7b7e\u9875\u7f13\u5b58\uff09...");
                appendPinnedSectionFromSnapshot(sb, filterPinnedSnapshotEntries(snapshot.getPinnedEntries(), questionKey),
                        maxItems, registryUpdatedAt > 0L);
            }
            else {
                LogUtils.info("AI workspace plans: side tab cache empty, falling back to disk scan.");
                report(progress, 4, 6, "\u6807\u7b7e\u9875\u7f13\u5b58\u4e3a\u7a7a\uff0c\u6b63\u5728\u626b\u63cf\u5bfc\u56fe\u76ee\u5f55\uff08\u53ef\u80fd\u8f83\u6162\uff09...");
                MindMapWorkspaceContextScanner.WorkspaceScanResult scanResult = MindMapWorkspaceContextScanner.scanAll();
                report(progress, 4, 6, "\u626b\u63cf\u5b8c\u6210\uff0c\u6574\u7406\u63d0\u9192\u4e0e\u5f85\u529e...");
                appendReminderSectionFromScan(sb, "\u5168\u90e8\u63d0\u9192",
                        filterReminderScanEntries(scanResult.oneTimeReminders, questionKey), maxItems);
                appendReminderSectionFromScan(sb, "\u5468\u671f\u63d0\u9192",
                        filterReminderScanEntries(scanResult.recurringReminders, questionKey), maxItems);
                appendTodoSectionFromScan(sb, filterTodoScanEntries(scanResult.todos, questionKey), maxItems);
                appendPinnedSectionFromSnapshot(sb, Collections.EMPTY_LIST, maxItems, false);
            }
        }
        catch (Exception e) {
            LogUtils.warn("Failed to collect workspace plans for AI: " + e.getMessage());
            if (sb.length() == 0) {
                return "\uff08\u6682\u65e0\u6cd5\u8bfb\u53d6\u5de5\u4f5c\u533a\u5b89\u6392\u6570\u636e\uff09";
            }
        }
        if (sb.length() == 0) {
            return "\uff08\u5f53\u524d\u6ca1\u6709\u63d0\u9192\u3001\u5f85\u529e\u6216\u9489\u9009\u8282\u70b9\uff09";
        }
        String result = truncate(sb.toString().trim(), maxChars);
        LogUtils.info("AI workspace plans collected from "
                + (usedTabSnapshot ? "side tabs" : "disk scan") + ": " + result.length() + " chars in "
                + (System.currentTimeMillis() - started) + " ms.");
        putCachedPlans(config, registryUpdatedAt, questionKey, result);
        return result;
    }

    private static void report(AiPromptBuildProgress progress, int step, int total, String label) {
        if (progress != null) {
            progress.onStep(step, total, label);
        }
    }

    private static String getCachedPlans(DocearAiConfig config, long registryUpdatedAt, String questionKey) {
        synchronized (CACHE_LOCK) {
            if (cachedPlans == null || cachedRegistryUpdatedAt != registryUpdatedAt) {
                return null;
            }
            if (!questionKey.equals(cachedQuestionKey)) {
                return null;
            }
            long age = System.currentTimeMillis() - cachedPlansAt;
            if (age > config.getWorkspacePlansCacheTtlMs()) {
                return null;
            }
            return cachedPlans;
        }
    }

    private static void putCachedPlans(DocearAiConfig config, long registryUpdatedAt, String questionKey,
            String result) {
        synchronized (CACHE_LOCK) {
            cachedPlans = result;
            cachedPlansAt = System.currentTimeMillis();
            cachedRegistryUpdatedAt = registryUpdatedAt;
            cachedQuestionKey = questionKey != null ? questionKey : "";
        }
    }

    private static List filterReminderEntries(List reminders, String question) {
        if (reminders == null || reminders.isEmpty()) {
            return reminders;
        }
        List terms = extractQuestionTerms(question);
        if (terms.isEmpty()) {
            return reminders;
        }
        List filtered = new ArrayList();
        for (int i = 0; i < reminders.size(); i++) {
            WorkspaceSideTabSnapshot.ReminderEntry item = (WorkspaceSideTabSnapshot.ReminderEntry) reminders.get(i);
            if (matchesAnyTerm(item.nodeText, terms)) {
                filtered.add(item);
            }
        }
        return filtered.isEmpty() ? reminders : filtered;
    }

    private static List filterTodoEntries(List todos, String question) {
        if (todos == null || todos.isEmpty()) {
            return todos;
        }
        List terms = extractQuestionTerms(question);
        if (terms.isEmpty()) {
            return todos;
        }
        List filtered = new ArrayList();
        for (int i = 0; i < todos.size(); i++) {
            WorkspaceSideTabSnapshot.TodoEntry item = (WorkspaceSideTabSnapshot.TodoEntry) todos.get(i);
            if (matchesAnyTerm(item.nodeText, terms)) {
                filtered.add(item);
            }
        }
        return filtered.isEmpty() ? todos : filtered;
    }

    private static List filterReminderScanEntries(List reminders, String question) {
        if (reminders == null || reminders.isEmpty()) {
            return reminders;
        }
        List terms = extractQuestionTerms(question);
        if (terms.isEmpty()) {
            return reminders;
        }
        List filtered = new ArrayList();
        for (int i = 0; i < reminders.size(); i++) {
            ReminderItem item = (ReminderItem) reminders.get(i);
            if (matchesAnyTerm(item.nodeText, terms)) {
                filtered.add(item);
            }
        }
        return filtered.isEmpty() ? reminders : filtered;
    }

    private static List filterTodoScanEntries(List todos, String question) {
        if (todos == null || todos.isEmpty()) {
            return todos;
        }
        List terms = extractQuestionTerms(question);
        if (terms.isEmpty()) {
            return todos;
        }
        List filtered = new ArrayList();
        for (int i = 0; i < todos.size(); i++) {
            TodoItem item = (TodoItem) todos.get(i);
            if (matchesAnyTerm(item.nodeText, terms)) {
                filtered.add(item);
            }
        }
        return filtered.isEmpty() ? todos : filtered;
    }

    private static List filterPinnedSnapshotEntries(List pinned, String question) {
        if (pinned == null || pinned.isEmpty()) {
            return pinned != null ? pinned : Collections.EMPTY_LIST;
        }
        List terms = extractQuestionTerms(question);
        if (terms.isEmpty()) {
            return pinned;
        }
        List filtered = new ArrayList();
        for (int i = 0; i < pinned.size(); i++) {
            WorkspaceSideTabSnapshot.PinnedEntry entry = (WorkspaceSideTabSnapshot.PinnedEntry) pinned.get(i);
            if (matchesAnyTerm(entry.nodeText, terms) || matchesAnyTerm(entry.tags, terms)) {
                filtered.add(entry);
            }
        }
        return filtered.isEmpty() ? pinned : filtered;
    }

    private static List extractQuestionTerms(String question) {
        List terms = new ArrayList();
        if (question == null) {
            return terms;
        }
        String q = question.trim();
        if (q.length() < 2) {
            return terms;
        }
        for (int i = 0; i < q.length(); i++) {
            if (!isChineseChar(q.charAt(i))) {
                continue;
            }
            for (int len = 4; len >= 2; len--) {
                if (i + len <= q.length()) {
                    String candidate = q.substring(i, i + len);
                    if (isStopWord(candidate) || terms.contains(candidate)) {
                        continue;
                    }
                    if (isChineseSequence(candidate)) {
                        terms.add(candidate);
                        i += len - 1;
                        break;
                    }
                }
            }
        }
        return terms;
    }

    private static boolean isChineseChar(char ch) {
        return ch >= 0x4E00 && ch <= 0x9FFF;
    }

    private static boolean isChineseSequence(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (!isChineseChar(text.charAt(i))) {
                return false;
            }
        }
        return text.length() >= 2;
    }

    private static boolean isStopWord(String word) {
        for (int i = 0; i < QUESTION_STOP_WORDS.length; i++) {
            if (QUESTION_STOP_WORDS[i].equals(word)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAnyTerm(String text, List terms) {
        if (text == null || text.trim().length() == 0) {
            return false;
        }
        String normalized = text.toLowerCase();
        for (int i = 0; i < terms.size(); i++) {
            String term = (String) terms.get(i);
            if (normalized.indexOf(term.toLowerCase()) >= 0) {
                return true;
            }
        }
        return false;
    }

    private static void appendReminderSection(StringBuilder sb, String title, List reminders, int maxItems) {
        sb.append("### ").append(title).append(" (\u5171 ").append(reminders.size()).append(" \u6761)\n");
        if (reminders.isEmpty()) {
            sb.append("\uff08\u65e0\uff09\n\n");
            return;
        }
        int limit = Math.min(reminders.size(), maxItems);
        for (int i = 0; i < limit; i++) {
            WorkspaceSideTabSnapshot.ReminderEntry item = (WorkspaceSideTabSnapshot.ReminderEntry) reminders.get(i);
            sb.append("- [").append(formatTimestamp(item.remindAt)).append("] ");
            sb.append(safeText(item.nodeText));
            appendCompactLocation(sb, item.mapFile, item.nodeId);
            if (item.recurring && item.remindType != null && item.remindType.length() > 0) {
                sb.append(" | \u5468\u671f:").append(item.remindType);
            }
            sb.append('\n');
        }
        appendOverflowNotice(sb, reminders.size(), limit);
        sb.append('\n');
    }

    private static void appendReminderSectionFromScan(StringBuilder sb, String title, List reminders, int maxItems) {
        sb.append("### ").append(title).append(" (\u5171 ").append(reminders.size()).append(" \u6761)\n");
        if (reminders.isEmpty()) {
            sb.append("\uff08\u65e0\uff09\n\n");
            return;
        }
        int limit = Math.min(reminders.size(), maxItems);
        for (int i = 0; i < limit; i++) {
            ReminderItem item = (ReminderItem) reminders.get(i);
            sb.append("- [").append(formatTimestamp(item.remindAt)).append("] ");
            sb.append(safeText(item.nodeText));
            appendCompactLocation(sb, item.mapFile, item.nodeId);
            if (item.recurring && item.remindType != null && item.remindType.length() > 0) {
                sb.append(" | \u5468\u671f:").append(item.remindType);
            }
            sb.append('\n');
        }
        appendOverflowNotice(sb, reminders.size(), limit);
        sb.append('\n');
    }

    private static void appendTodoSection(StringBuilder sb, List todos, int maxItems) {
        sb.append("### \u5168\u90e8\u5f85\u529e (\u5171 ").append(todos.size()).append(" \u6761)\n");
        if (todos.isEmpty()) {
            sb.append("\uff08\u65e0\uff09\n\n");
            return;
        }
        int limit = Math.min(todos.size(), maxItems);
        for (int i = 0; i < limit; i++) {
            WorkspaceSideTabSnapshot.TodoEntry item = (WorkspaceSideTabSnapshot.TodoEntry) todos.get(i);
            sb.append("- ").append(safeText(item.nodeText));
            appendCompactLocation(sb, item.mapFile, item.nodeId);
            sb.append('\n');
        }
        appendOverflowNotice(sb, todos.size(), limit);
        sb.append('\n');
    }

    private static void appendTodoSectionFromScan(StringBuilder sb, List todos, int maxItems) {
        sb.append("### \u5168\u90e8\u5f85\u529e (\u5171 ").append(todos.size()).append(" \u6761)\n");
        if (todos.isEmpty()) {
            sb.append("\uff08\u65e0\uff09\n\n");
            return;
        }
        int limit = Math.min(todos.size(), maxItems);
        for (int i = 0; i < limit; i++) {
            TodoItem item = (TodoItem) todos.get(i);
            sb.append("- ").append(safeText(item.nodeText));
            appendCompactLocation(sb, item.mapFile, item.nodeId);
            sb.append('\n');
        }
        appendOverflowNotice(sb, todos.size(), limit);
        sb.append('\n');
    }

    private static void appendPinnedSectionFromSnapshot(StringBuilder sb, List pinned, int maxItems, boolean tabLoaded) {
        sb.append("### \u6211\u7684\u9489\u9009 (\u5171 ").append(pinned.size()).append(" \u6761)\n");
        if (pinned.isEmpty()) {
            if (!tabLoaded) {
                sb.append("\uff08\u9489\u9009\u6570\u636e\u5c1a\u672a\u52a0\u8f7d\uff0c\u8bf7\u5148\u6253\u5f00\u4fa7\u680f\u300c\u6211\u7684\u9489\u9009\u300d\u6807\u7b7e\u9875\uff09\n");
            }
            else {
                sb.append("\uff08\u65e0\uff09\n");
            }
            return;
        }
        int limit = Math.min(pinned.size(), maxItems);
        for (int i = 0; i < limit; i++) {
            WorkspaceSideTabSnapshot.PinnedEntry entry = (WorkspaceSideTabSnapshot.PinnedEntry) pinned.get(i);
            sb.append("- ").append(safeText(entry.nodeText));
            appendCompactLocation(sb, entry.mapFile, entry.nodeId);
            if (entry.tags != null && entry.tags.trim().length() > 0) {
                sb.append(" | \u6807\u7b7e:").append(entry.tags.trim());
            }
            sb.append('\n');
        }
        appendOverflowNotice(sb, pinned.size(), limit);
    }

    private static void appendOverflowNotice(StringBuilder sb, int total, int included) {
        if (total > included) {
            sb.append("[\u8fd8\u6709 ").append(total - included).append(" \u6761\u672a\u5c55\u793a]\n");
        }
    }

    private static void appendCompactLocation(StringBuilder sb, File mapFile, String nodeId) {
        if (mapFile != null) {
            sb.append(" | \u5bfc\u56fe:").append(mapFile.getAbsolutePath());
        }
        if (nodeId != null && nodeId.length() > 0) {
            sb.append(" | \u8282\u70b9:").append(nodeId);
        }
    }

    private static String formatTimestamp(long timestamp) {
        synchronized (DATE_TIME_FORMAT) {
            return DATE_TIME_FORMAT.format(new Date(timestamp));
        }
    }

    private static String safeText(String text) {
        if (text == null || text.trim().length() == 0) {
            return "\uff08\u65e0\u6807\u9898\u8282\u70b9\uff09";
        }
        return text.trim().replace('\n', ' ');
    }

    private static String truncate(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content != null ? content : "";
        }
        return content.substring(0, maxLength)
                + "\n\n[\u5de5\u4f5c\u533a\u5b89\u6392\u5185\u5bb9\u8fc7\u957f\uff0c\u5df2\u622a\u65ad\u81f3 " + maxLength
                + " \u5b57\u7b26]";
    }
}
