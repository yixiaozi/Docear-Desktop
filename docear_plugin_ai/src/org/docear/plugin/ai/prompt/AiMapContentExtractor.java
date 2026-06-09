package org.docear.plugin.ai.prompt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.text.TextController;

/**
 * 在 Docear 进程内读取思维导图内容，避免依赖 Copilot CLI 访问本地文件。
 */
public final class AiMapContentExtractor {

    private static final int MAX_CONTENT_LENGTH = 60000;

    private AiMapContentExtractor() {
    }

    public static String extractMapContent(MapModel map) {
        if (map == null) {
            return "\uff08\u672a\u6253\u5f00\u601d\u7ef4\u5bfc\u56fe\uff09";
        }

        String fromMemory = extractFromMapModel(map);
        if (fromMemory != null && fromMemory.trim().length() > 0) {
            return truncate(fromMemory);
        }

        File file = map.getFile();
        if (file != null && file.exists()) {
            String fromFile = readMindMapFile(file);
            if (fromFile != null && fromFile.trim().length() > 0) {
                return truncate(fromFile);
            }
        }

        return "\uff08\u5bfc\u56fe\u65e0\u53ef\u7528\u5185\u5bb9\u6216\u5c1a\u672a\u4fdd\u5b58\uff09";
    }

    private static String extractFromMapModel(MapModel map) {
        NodeModel root = map.getRootNode();
        if (root == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        appendNode(root, sb, 0);
        return sb.toString();
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
        if (text.length() > 0) {
            for (int i = 0; i < depth; i++) {
                sb.append("  ");
            }
            sb.append("- ").append(text).append('\n');
        }

        List<NodeModel> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            appendNode(children.get(i), sb, depth + 1);
        }
    }

    private static String readMindMapFile(File file) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            LogUtils.warn("Failed to read mind map file: " + file + ", " + e.getMessage());
            return "";
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static String truncate(String content) {
        if (content == null) {
            return "";
        }
        if (content.length() <= MAX_CONTENT_LENGTH) {
            return content;
        }
        return content.substring(0, MAX_CONTENT_LENGTH)
                + "\n\n[\u5185\u5bb9\u8fc7\u957f\uff0c\u5df2\u622a\u65ad\u81f3 " + MAX_CONTENT_LENGTH + " \u5b57\u7b26]";
    }
}
