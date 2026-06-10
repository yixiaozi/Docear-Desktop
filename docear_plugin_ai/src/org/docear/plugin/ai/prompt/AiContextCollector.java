package org.docear.plugin.ai.prompt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.docear.plugin.ai.DocearAiConfig;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.link.NodeLinks;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.text.TextController;

/**
 * 在 Docear 进程内尽可能全面地收集导图及关联文件内容。
 */
public final class AiContextCollector {

    private static final Pattern WINDOWS_PATH = Pattern.compile("[A-Za-z]:[\\\\/][^\\s<>\"|?*\\n\\r]{2,}");
    private static final Pattern UNIX_PATH = Pattern.compile("(?:/[\\w.$~_-]+)+");
    private static final String[] TEXT_EXTENSIONS = new String[] {
            ".mm", ".txt", ".md", ".markdown", ".java", ".py", ".js", ".ts", ".json", ".xml",
            ".properties", ".bib", ".csv", ".html", ".htm", ".yaml", ".yml", ".sql", ".ini", ".cfg", ".log"
    };

    private AiContextCollector() {
    }

    public static ContextBundle collect(MapModel map, String userQuestion) {
        DocearAiConfig config = new DocearAiConfig();
        int maxFiles = config.getMaxLinkedFiles();
        int maxFileBytes = config.getMaxFileSizeBytes();
        int maxTotalChars = config.getMaxTotalContextChars();

        String mapStructure = extractMapStructure(map);
        Set<File> files = collectReferencedFiles(map, userQuestion);
        StringBuilder referenced = new StringBuilder();
        int totalChars = mapStructure.length();
        int filesIncluded = 0;

        for (File file : files) {
            if (filesIncluded >= maxFiles || totalChars >= maxTotalChars) {
                break;
            }
            String content = readTextFile(file, maxFileBytes);
            if (content == null || content.trim().length() == 0) {
                continue;
            }
            if (totalChars + content.length() > maxTotalChars) {
                int remaining = maxTotalChars - totalChars;
                if (remaining <= 200) {
                    break;
                }
                content = content.substring(0, remaining)
                        + "\n[\u5185\u5bb9\u8fc7\u957f\uff0c\u5df2\u622a\u65ad]";
            }
            if (referenced.length() > 0) {
                referenced.append("\n\n");
            }
            referenced.append("=== \u6587\u4ef6: ").append(file.getAbsolutePath()).append(" ===\n");
            referenced.append(content);
            totalChars += content.length();
            filesIncluded++;
        }

        if (files.size() > filesIncluded) {
            referenced.append("\n\n[\u8fd8\u6709 ").append(files.size() - filesIncluded)
                    .append(" \u4e2a\u5173\u8054\u6587\u4ef6\u672a\u5c55\u5f00\uff0c\u5df2\u8fbe\u4e0a\u9650]");
        }

        return new ContextBundle(mapStructure, referenced.toString(), filesIncluded, files.size());
    }

    private static String extractMapStructure(MapModel map) {
        if (map == null) {
            return "\uff08\u672a\u6253\u5f00\u601d\u7ef4\u5bfc\u56fe\uff09";
        }
        NodeModel root = map.getRootNode();
        if (root == null) {
            return "\uff08\u5bfc\u56fe\u65e0\u6839\u8282\u70b9\uff09";
        }
        StringBuilder sb = new StringBuilder();
        appendNode(root, sb, 0);
        return truncate(sb.toString(), 60000);
    }

    private static void appendNode(NodeModel node, StringBuilder sb, int depth) {
        if (node == null) {
            return;
        }
        String text = TextController.getController().getPlainTextContent(node);
        if (text == null) {
            text = "";
        }
        text = text.trim();
        URI link = NodeLinks.getValidLink(node);
        if (text.length() > 0 || link != null) {
            for (int i = 0; i < depth; i++) {
                sb.append("  ");
            }
            sb.append("- ");
            if (text.length() > 0) {
                sb.append(text);
            }
            if (link != null) {
                if (text.length() > 0) {
                    sb.append(" ");
                }
                sb.append("[\u94fe\u63a5: ").append(link.toString()).append(']');
            }
            sb.append('\n');
        }
        List<NodeModel> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            appendNode(children.get(i), sb, depth + 1);
        }
    }

    private static Set<File> collectReferencedFiles(MapModel map, String userQuestion) {
        LinkedHashSet<File> files = new LinkedHashSet<File>();
        File mapFile = map != null ? map.getFile() : null;
        File baseDir = mapFile != null ? mapFile.getParentFile() : null;

        if (mapFile != null && mapFile.exists()) {
            files.add(mapFile);
        }

        if (map != null) {
            collectFilesFromNode(map.getRootNode(), baseDir, files);
        }
        collectPathsFromText(userQuestion, baseDir, files);

        File promptTemplate = new File(new DocearAiConfig().getPromptTemplateFile());
        if (promptTemplate.exists()) {
            files.add(promptTemplate);
        }

        return files;
    }

    private static void collectFilesFromNode(NodeModel node, File baseDir, Set<File> files) {
        if (node == null) {
            return;
        }
        URI link = NodeLinks.getValidLink(node);
        addFileFromUri(link, baseDir, files);

        String text = TextController.getController().getPlainTextContent(node);
        collectPathsFromText(text, baseDir, files);

        List<NodeModel> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            collectFilesFromNode(children.get(i), baseDir, files);
        }
    }

    private static void collectPathsFromText(String text, File baseDir, Set<File> files) {
        if (text == null || text.trim().length() == 0) {
            return;
        }
        Matcher windowsMatcher = WINDOWS_PATH.matcher(text);
        while (windowsMatcher.find()) {
            addCandidateFile(new File(windowsMatcher.group()), baseDir, files);
        }
        Matcher unixMatcher = UNIX_PATH.matcher(text);
        while (unixMatcher.find()) {
            addCandidateFile(new File(unixMatcher.group()), baseDir, files);
        }
    }

    private static void addFileFromUri(URI uri, File baseDir, Set<File> files) {
        if (uri == null) {
            return;
        }
        try {
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                addCandidateFile(new File(uri), baseDir, files);
            } else {
                String raw = uri.toString();
                if (raw.length() > 2 && raw.charAt(1) == ':') {
                    addCandidateFile(new File(raw), baseDir, files);
                } else if (raw.startsWith("/") || raw.matches("[A-Za-z]:.*")) {
                    addCandidateFile(new File(raw), baseDir, files);
                }
            }
        } catch (Exception e) {
            LogUtils.warn("Failed to resolve link: " + uri);
        }
    }

    private static void addCandidateFile(File candidate, File baseDir, Set<File> files) {
        if (candidate == null) {
            return;
        }
        File resolved = candidate;
        if (!resolved.isAbsolute() && baseDir != null) {
            resolved = new File(baseDir, candidate.getPath());
        }
        if (!resolved.exists() || !resolved.isFile() || !isReadableTextFile(resolved)) {
            return;
        }
        files.add(resolved.getAbsoluteFile());
    }

    private static boolean isReadableTextFile(File file) {
        String name = file.getName().toLowerCase();
        for (int i = 0; i < TEXT_EXTENSIONS.length; i++) {
            if (name.endsWith(TEXT_EXTENSIONS[i])) {
                return true;
            }
        }
        return false;
    }

    private static String readTextFile(File file, int maxBytes) {
        BufferedReader reader = null;
        try {
            long size = file.length();
            if (size > maxBytes) {
                return readHead(file, maxBytes) + "\n[\u6587\u4ef6\u8fc7\u5927\uff0c\u5df2\u8bfb\u53d6\u524d " + maxBytes + " \u5b57\u8282]";
            }
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            LogUtils.warn("Failed to read file for AI context: " + file + ", " + e.getMessage());
            return "[\u65e0\u6cd5\u8bfb\u53d6\u6587\u4ef6: " + file.getAbsolutePath() + "]";
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static String readHead(File file, int maxBytes) throws Exception {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            char[] buffer = new char[maxBytes];
            int read = reader.read(buffer, 0, maxBytes);
            if (read <= 0) {
                return "";
            }
            return new String(buffer, 0, read);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private static String truncate(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content != null ? content : "";
        }
        return content.substring(0, maxLength)
                + "\n\n[\u5185\u5bb9\u8fc7\u957f\uff0c\u5df2\u622a\u65ad\u81f3 " + maxLength + " \u5b57\u7b26]";
    }

    public static final class ContextBundle {
        private final String mapStructure;
        private final String referencedFiles;
        private final int filesIncluded;
        private final int filesDiscovered;

        public ContextBundle(String mapStructure, String referencedFiles, int filesIncluded, int filesDiscovered) {
            this.mapStructure = mapStructure;
            this.referencedFiles = referencedFiles;
            this.filesIncluded = filesIncluded;
            this.filesDiscovered = filesDiscovered;
        }

        public String getMapStructure() {
            return mapStructure;
        }

        public String getReferencedFiles() {
            return referencedFiles;
        }

        public String getCombinedMapContent() {
            StringBuilder sb = new StringBuilder();
            sb.append("--- \u601d\u7ef4\u5bfc\u56fe\u7ed3\u6784 ---\n");
            sb.append(mapStructure);
            if (referencedFiles != null && referencedFiles.trim().length() > 0) {
                sb.append("\n\n--- \u5173\u8054\u6587\u4ef6\u5185\u5bb9 ---\n");
                sb.append(referencedFiles);
            }
            return sb.toString();
        }

        public int getFilesIncluded() {
            return filesIncluded;
        }

        public int getFilesDiscovered() {
            return filesDiscovered;
        }
    }
}
