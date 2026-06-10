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

    public AiChatContextInfo(String mapTitle, String mapPath, String selectedNodeText,
            int filesIncluded, int filesDiscovered, boolean backendReady,
            int redactionCount, String statusHint) {
        this.mapTitle = mapTitle != null ? mapTitle : "\u65e0";
        this.mapPath = mapPath != null ? mapPath : "";
        this.selectedNodeText = selectedNodeText != null ? selectedNodeText : "";
        this.filesIncluded = filesIncluded;
        this.filesDiscovered = filesDiscovered;
        this.backendReady = backendReady;
        this.redactionCount = redactionCount;
        this.statusHint = statusHint != null ? statusHint : "";
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
        sb.append("  |  ").append(backendReady ? "Copilot \u5c31\u7eea" : "Copilot \u672a\u5c31\u7eea");
        if (statusHint.length() > 0) {
            sb.append("  |  ").append(statusHint);
        }
        return sb.toString();
    }

    public String formatDetailText() {
        StringBuilder sb = new StringBuilder();
        sb.append("\u5bfc\u56fe\u6807\u9898: ").append(mapTitle).append('\n');
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
        sb.append("AI \u540e\u7aef: ").append(backendReady ? "Copilot CLI \u5c31\u7eea" : "Copilot CLI \u672a\u68c0\u6d4b");
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
