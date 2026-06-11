package org.docear.plugin.ai.ui;

/**
 * AI 聊天状态栏上下文摘要。
 */
public class AiChatContextInfo {

    private final String mapTitle;
    private final String mapPath;
    private final String selectedNodeText;
    private final int filesIncluded;
    private final int filesDiscovered;
    private final boolean backendReady;
    private final int redactionCount;
    private final String statusHint;
    private final int monthlyUsageCount;
    private final int monthlyQuota;
    private final int todayUsageCount;

    public AiChatContextInfo(String mapTitle, String mapPath, String selectedNodeText,
            int filesIncluded, int filesDiscovered, boolean backendReady,
            int redactionCount, String statusHint) {
        this(mapTitle, mapPath, selectedNodeText, filesIncluded, filesDiscovered,
                backendReady, redactionCount, statusHint, 0, 0, 0);
    }

    public AiChatContextInfo(String mapTitle, String mapPath, String selectedNodeText,
            int filesIncluded, int filesDiscovered, boolean backendReady,
            int redactionCount, String statusHint, int monthlyUsageCount,
            int monthlyQuota, int todayUsageCount) {
        this.mapTitle = mapTitle != null ? mapTitle : "\u65e0";
        this.mapPath = mapPath != null ? mapPath : "";
        this.selectedNodeText = selectedNodeText != null ? selectedNodeText : "";
        this.filesIncluded = filesIncluded;
        this.filesDiscovered = filesDiscovered;
        this.backendReady = backendReady;
        this.redactionCount = redactionCount;
        this.statusHint = statusHint != null ? statusHint : "";
        this.monthlyUsageCount = monthlyUsageCount;
        this.monthlyQuota = monthlyQuota;
        this.todayUsageCount = todayUsageCount;
    }

    public String getMapTitle() {
        return mapTitle;
    }

    public String getMapPath() {
        return mapPath;
    }

    public String getSelectedNodeText() {
        return selectedNodeText;
    }

    public int getFilesIncluded() {
        return filesIncluded;
    }

    public int getFilesDiscovered() {
        return filesDiscovered;
    }

    public boolean isBackendReady() {
        return backendReady;
    }

    public int getRedactionCount() {
        return redactionCount;
    }

    public String getStatusHint() {
        return statusHint;
    }

    public int getMonthlyUsageCount() {
        return monthlyUsageCount;
    }

    public int getMonthlyQuota() {
        return monthlyQuota;
    }

    public int getTodayUsageCount() {
        return todayUsageCount;
    }

    public boolean isQuotaNear() {
        return monthlyQuota > 0 && monthlyUsageCount >= (int) (monthlyQuota * 0.8d);
    }

    public boolean isQuotaExceeded() {
        return monthlyQuota > 0 && monthlyUsageCount >= monthlyQuota;
    }

    public String formatStatusLine() {
        StringBuilder sb = new StringBuilder();
        sb.append("\u5bfc\u56fe: ").append(truncate(mapTitle, 18));
        if (selectedNodeText.length() > 0) {
            sb.append("  |  \u9009\u4e2d: ").append(truncate(selectedNodeText, 16));
        } else {
            sb.append("  |  \u9009\u4e2d: (\u65e0)");
        }
        sb.append("  |  \u6587\u4ef6: ").append(filesIncluded);
        if (filesDiscovered > filesIncluded) {
            sb.append("/").append(filesDiscovered);
        }
        if (redactionCount > 0) {
            sb.append("  |  \u8131\u654f: ").append(redactionCount);
        }
        sb.append("  |  \u7528\u91cf: \u672c\u6708 ").append(monthlyUsageCount);
        if (monthlyQuota > 0) {
            sb.append("/").append(monthlyQuota);
        }
        sb.append(" \u4eca\u65e5 ").append(todayUsageCount);
        if (isQuotaExceeded()) {
            sb.append(" \u26a0\u914d\u989d\u5df2\u7528\u5b8c");
        } else if (isQuotaNear()) {
            sb.append(" \u26a0\u63a5\u8fd1\u914d\u989d");
        }
        sb.append("  |  ").append(backendReady ? "Copilot \u5c31\u7eea" : "Copilot \u672a\u5c31\u7eea");
        if (statusHint.length() > 0) {
            sb.append("  |  ").append(statusHint);
        }
        return sb.toString();
    }

    public String formatDetailText() {
        StringBuilder sb = new StringBuilder();
        sb.append("\u5bfc\u56fe\u6807\u9891: ").append(mapTitle).append('\n');
        sb.append("\u5bfc\u56fe\u8def\u5f84: ").append(mapPath).append('\n');
        if (selectedNodeText.length() > 0) {
            sb.append("\u9009\u4e2d\u8282\u70b9: ").append(selectedNodeText).append('\n');
        }
        sb.append("\u5df2\u8bfb\u5173\u8054\u6587\u4ef6: ").append(filesIncluded);
        if (filesDiscovered > filesIncluded) {
            sb.append(" / ").append(filesDiscovered).append(" (\u90e8\u5206\u672a\u5c55\u5f00)");
        }
        sb.append('\n');
        if (redactionCount > 0) {
            sb.append("\u8131\u654f\u5904\u6570: ").append(redactionCount).append('\n');
        }
        sb.append("AI \u540e\u7aef: ").append(backendReady ? "Copilot CLI \u5c31\u7eea" : "Copilot CLI \u672a\u68c0\u6d4b").append('\n');
        sb.append("\u672c\u6708\u8c03\u7528\u6b21\u6570: ").append(monthlyUsageCount);
        if (monthlyQuota > 0) {
            sb.append(" / ").append(monthlyQuota);
        }
        sb.append("  \u4eca\u65e5: ").append(todayUsageCount).append('\n');
        if (isQuotaExceeded()) {
            sb.append("\u26a0\u914d\u989d\u5df2\u7528\u5b8c\uff01\u8bf7\u8003\u8651\u66f4\u6362\u8d26\u53f7\u6216\u5347\u7ea7\u8ba2\u9605\u3002\n");
        } else if (isQuotaNear()) {
            sb.append("\u26a0\u63a5\u8fd1\u914d\u989d\u4e0a\u9650\uff0c\u8bf7\u6ce8\u610f\u63a7\u5236\u8c03\u7528\u3002\n");
        }
        if (statusHint.length() > 0) {
            sb.append('\n').append(statusHint);
        }
        return sb.toString();
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        String trimmed = text.replace('\n', ' ').trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max) + "...";
    }
}
