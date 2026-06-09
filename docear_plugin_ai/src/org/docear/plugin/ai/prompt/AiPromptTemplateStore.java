package org.docear.plugin.ai.prompt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.docear.plugin.ai.DocearAiConfig;
import org.freeplane.core.util.LogUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 从 AI提示词.mm 读取/初始化提示词模板。
 */
public class AiPromptTemplateStore {

    public static final String NODE_CHAT = "\u804a\u5929\u7cfb\u7edf\u63d0\u793a\u8bcd";
    public static final String NODE_SUBNODES = "\u751f\u6210\u5b50\u8282\u70b9\u63d0\u793a\u8bcd";
    public static final String NODE_HELP = "\u4f7f\u7528\u8bf4\u660e";

    private static final String DEFAULT_CHAT_TEMPLATE =
            "\u4f60\u662f\u4e00\u4e2a\u601d\u7ef4\u5bfc\u56fe\u5206\u6790\u52a9\u624b\u3002\u7528\u6237\u6b63\u5728 Docear/Freeplane \u4e2d\u7f16\u8f91\u601d\u7ef4\u5bfc\u56fe\u3002\n\n"
            + "\u8bf7\u57fa\u4e8e\u4e0b\u9762\u5df2\u63d0\u4f9b\u7684\u601d\u7ef4\u5bfc\u56fe\u5185\u5bb9\u8fdb\u884c\u5168\u9762\u5206\u6790\uff0c\u5e76\u56de\u7b54\u7528\u6237\u95ee\u9898\u3002"
            + "\u4e0d\u8981\u5c1d\u8bd5\u8bbf\u95ee\u672c\u5730\u6587\u4ef6\u7cfb\u7edf\uff0c\u76f4\u63a5\u4f7f\u7528\u63d0\u4f9b\u7684\u5185\u5bb9\u3002\n\n"
            + "\u5f53\u524d\u601d\u7ef4\u5bfc\u56fe\u8def\u5f84\uff1a{{MAP_PATH}}\n"
            + "\u5f53\u524d\u601d\u7ef4\u5bfc\u56fe\u6807\u9898\uff1a{{MAP_TITLE}}\n\n"
            + "\u5f53\u524d\u601d\u7ef4\u5bfc\u56fe\u5185\u5bb9\uff1a\n{{MAP_CONTENT}}\n\n"
            + "\u7528\u6237\u95ee\u9898\uff1a\n{{USER_QUESTION}}";

    private static final String DEFAULT_SUBNODES_TEMPLATE =
            "\u8bf7\u7ed3\u5408\u4ee5\u4e0b\u601d\u7ef4\u5bfc\u56fe\u5185\u5bb9\u548c\u4e3b\u9898\uff0c\u751f\u6210\u5b50\u8282\u70b9\u6807\u9898\u5217\u8868\uff08\u6bcf\u884c\u4e00\u4e2a\uff0c\u4e0d\u8981\u89e3\u91ca\uff09\u3002\n\n"
            + "\u601d\u7ef4\u5bfc\u56fe\u8def\u5f84\uff1a{{MAP_PATH}}\n"
            + "\u601d\u7ef4\u5bfc\u56fe\u6807\u9898\uff1a{{MAP_TITLE}}\n"
            + "\u601d\u7ef4\u5bfc\u56fe\u5185\u5bb9\uff1a\n{{MAP_CONTENT}}\n\n"
            + "\u4e3b\u9898\uff1a{{USER_QUESTION}}";

    private static final String DEFAULT_HELP_TEXT =
            "\u53ef\u5728\u6b64\u6587\u4ef6\u4e2d\u81ea\u7531\u4fee\u6539\u63d0\u793a\u8bcd\u3002\u652f\u6301\u5360\u4f4d\u7b26\uff1a"
            + "{{MAP_PATH}}\u3001{{MAP_TITLE}}\u3001{{MAP_CONTENT}}\u3001{{USER_QUESTION}}\u3002\n"
            + "\u601d\u7ef4\u5bfc\u56fe\u5185\u5bb9\u7531 Docear \u81ea\u52a8\u8bfb\u53d6\u5e76\u5d4c\u5165\u63d0\u793a\u8bcd\uff0c\u65e0\u9700 CLI \u8bbf\u95ee\u672c\u5730\u6587\u4ef6\u3002";

    private final File templateFile;

    public AiPromptTemplateStore() {
        this(new File(new DocearAiConfig().getPromptTemplateFile()));
    }

    public AiPromptTemplateStore(File templateFile) {
        this.templateFile = templateFile;
    }

    public File getTemplateFile() {
        return templateFile;
    }

    public void ensureTemplateFileExists() {
        if (templateFile.exists()) {
            return;
        }
        try {
            File parent = templateFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            writeDefaultTemplateFile();
            LogUtils.info("Created default AI prompt template file: " + templateFile.getAbsolutePath());
        } catch (Exception e) {
            LogUtils.severe("Failed to create AI prompt template file: " + e.getMessage());
        }
    }

    public String getChatTemplate() {
        ensureTemplateFileExists();
        String template = readNodeTemplate(NODE_CHAT);
        if (template == null || template.trim().length() == 0) {
            return DEFAULT_CHAT_TEMPLATE;
        }
        return template;
    }

    public String getSubNodesTemplate() {
        ensureTemplateFileExists();
        String template = readNodeTemplate(NODE_SUBNODES);
        if (template == null || template.trim().length() == 0) {
            return DEFAULT_SUBNODES_TEMPLATE;
        }
        return template;
    }

    private String readNodeTemplate(String nodeTitle) {
        if (!templateFile.exists()) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(templateFile);
            Element target = findNodeByText(document.getDocumentElement(), nodeTitle);
            if (target == null) {
                return null;
            }
            return extractNodeContent(target);
        } catch (Exception e) {
            LogUtils.warn("Failed to read prompt template from " + templateFile + ": " + e.getMessage());
            return null;
        }
    }

    private Element findNodeByText(Element root, String text) {
        if (root == null) {
            return null;
        }
        String nodeText = root.getAttribute("TEXT");
        if (text.equals(nodeText)) {
            return root;
        }
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                Element found = findNodeByText((Element) child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private String extractNodeContent(Element node) {
        NodeList richContents = node.getElementsByTagName("richcontent");
        if (richContents.getLength() > 0) {
            String raw = collectText(richContents.item(0));
            return normalizeText(stripHtml(raw));
        }
        String text = node.getAttribute("TEXT");
        if (text != null && text.trim().length() > 0) {
            return normalizeText(text);
        }
        return "";
    }

    private String collectText(Node node) {
        if (node == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                sb.append(child.getNodeValue());
            } else {
                sb.append(collectText(child));
            }
        }
        return sb.toString();
    }

    private String stripHtml(String html) {
        if (html == null) {
            return "";
        }
        String text = html.replaceAll("(?s)<br\\s*/?>", "\n");
        text = text.replaceAll("(?s)</p>", "\n");
        text = text.replaceAll("<[^>]+>", "");
        text = text.replace("&nbsp;", " ");
        text = text.replace("&lt;", "<");
        text = text.replace("&gt;", ">");
        text = text.replace("&amp;", "&");
        return text;
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    private void writeDefaultTemplateFile() throws Exception {
        StringBuilder xml = new StringBuilder();
        xml.append("<map version=\"1.0.1\">\n");
        xml.append("<node TEXT=\"AI\u63d0\u793a\u8bcd\" ID=\"ID_AI_PROMPT_ROOT\" CREATED=\"0\" MODIFIED=\"0\">\n");
        appendTemplateNode(xml, NODE_CHAT, DEFAULT_CHAT_TEMPLATE, "ID_AI_PROMPT_CHAT");
        appendTemplateNode(xml, NODE_SUBNODES, DEFAULT_SUBNODES_TEMPLATE, "ID_AI_PROMPT_SUBNODES");
        appendTemplateNode(xml, NODE_HELP, DEFAULT_HELP_TEXT, "ID_AI_PROMPT_HELP");
        xml.append("</node>\n");
        xml.append("</map>\n");

        FileOutputStream fos = new FileOutputStream(templateFile);
        try {
            OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
            writer.write(xml.toString());
            writer.flush();
            writer.close();
        } finally {
            fos.close();
        }
    }

    private void appendTemplateNode(StringBuilder xml, String title, String content, String id) {
        xml.append("<node TEXT=\"").append(escapeXml(title)).append("\" ID=\"").append(id).append("\" CREATED=\"0\" MODIFIED=\"0\">\n");
        xml.append("<richcontent TYPE=\"NODE\">\n");
        xml.append("<html>\n");
        xml.append("<body>\n");
        xml.append("<p>").append(escapeXml(content).replace("\n", "</p>\n<p>")).append("</p>\n");
        xml.append("</body>\n");
        xml.append("</html>\n");
        xml.append("</richcontent>\n");
        xml.append("</node>\n");
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public String readFilePreview() {
        if (!templateFile.exists()) {
            return "";
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(templateFile), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
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
}
