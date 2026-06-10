package org.docear.plugin.ai.prompt;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.text.TextController;

/**
 * AI 提示词导图中的系统节点定义。
 */
public final class AiPromptTemplateNodes {

    public static final String ROOT_TEXT = "AI\u63d0\u793a\u8bcd";
    public static final String NODE_CHAT = "\u804a\u5929\u7cfb\u7edf\u63d0\u793a\u8bcd";
    public static final String NODE_SUBNODES = "\u751f\u6210\u5b50\u8282\u70b9\u63d0\u793a\u8bcd";
    public static final String NODE_KEYWORDS = "\u5173\u952e\u8bcd\u5e93";
    public static final String NODE_HELP = "\u4f7f\u7528\u8bf4\u660e";

    public static final String ID_ROOT = "ID_AI_PROMPT_ROOT";
    public static final String ID_CHAT = "ID_AI_PROMPT_CHAT";
    public static final String ID_SUBNODES = "ID_AI_PROMPT_SUBNODES";
    public static final String ID_KEYWORDS = "ID_AI_PROMPT_KEYWORDS";
    public static final String ID_HELP = "ID_AI_PROMPT_HELP";

    public static final String PROTECTED_BACKGROUND_COLOR = "#ccffcc";

    private static final Set<String> PROTECTED_IDS = new HashSet<String>();

    static {
        PROTECTED_IDS.add(ID_ROOT);
        PROTECTED_IDS.add(ID_CHAT);
        PROTECTED_IDS.add(ID_SUBNODES);
        PROTECTED_IDS.add(ID_KEYWORDS);
        PROTECTED_IDS.add(ID_HELP);
    }

    private AiPromptTemplateNodes() {
    }

    public static boolean isProtectedNode(NodeModel node) {
        if (node == null) {
            return false;
        }
        String id = node.getID();
        if (id != null && PROTECTED_IDS.contains(id)) {
            return true;
        }
        String text = TextController.getController().getPlainTextContent(node);
        if (text == null) {
            return false;
        }
        return NODE_CHAT.equals(text) || NODE_SUBNODES.equals(text)
                || NODE_KEYWORDS.equals(text) || NODE_HELP.equals(text)
                || (node.isRoot() && ROOT_TEXT.equals(text));
    }

    public static boolean isPromptTemplateMap(MapModel map, File templateFile) {
        if (map == null || templateFile == null) {
            return false;
        }
        File mapFile = map.getFile();
        if (mapFile == null) {
            return false;
        }
        return mapFile.getAbsolutePath().equalsIgnoreCase(templateFile.getAbsolutePath());
    }

    public static String getProtectedNodeDescription() {
        return ROOT_TEXT + "\u3001" + NODE_CHAT + "\u3001" + NODE_SUBNODES + "\u3001"
                + NODE_KEYWORDS + "\u3001" + NODE_HELP;
    }
}
