package org.docear.plugin.ai.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * 将常见 Markdown 转为 HTML，供 JTextPane 显示。
 */
public final class AiMarkdownRenderer {

    private AiMarkdownRenderer() {
    }

    public static String toHtml(String markdown) {
        if (markdown == null || markdown.length() == 0) {
            return "<html><body><p></p></body></html>";
        }
        String text = escapeHtml(markdown);
        text = text.replace("\r\n", "\n").replace('\r', '\n');

        StringBuilder body = new StringBuilder();
        String[] lines = text.split("\n", -1);
        boolean inCode = false;
        StringBuilder codeBlock = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("```")) {
                if (!inCode) {
                    inCode = true;
                    codeBlock.setLength(0);
                } else {
                    inCode = false;
                    body.append("<pre style='background:#f4f4f4;padding:6px;border-radius:4px;"
                            + "white-space:pre-wrap;word-wrap:break-word;overflow-wrap:break-word;'>")
                            .append(codeBlock).append("</pre>");
                }
                continue;
            }
            if (inCode) {
                if (codeBlock.length() > 0) {
                    codeBlock.append('\n');
                }
                codeBlock.append(line);
                continue;
            }
            if (line.trim().length() == 0) {
                body.append("<br/>");
                continue;
            }
            String formatted = formatInline(line);
            if (line.startsWith("### ")) {
                body.append("<h4>").append(formatInline(line.substring(4))).append("</h4>");
            } else if (line.startsWith("## ")) {
                body.append("<h3>").append(formatInline(line.substring(3))).append("</h3>");
            } else if (line.startsWith("# ")) {
                body.append("<h2>").append(formatInline(line.substring(2))).append("</h2>");
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                body.append("<li>").append(formatInline(line.substring(2))).append("</li>");
            } else if (line.matches("\\d+\\.\\s+.*")) {
                int dot = line.indexOf('.');
                body.append("<li>").append(formatInline(line.substring(dot + 1).trim())).append("</li>");
            } else {
                body.append("<p style='margin:2px 0;word-wrap:break-word;overflow-wrap:break-word;'>")
                        .append(formatted).append("</p>");
            }
        }
        if (inCode && codeBlock.length() > 0) {
            body.append("<pre style='background:#f4f4f4;padding:6px;border-radius:4px;"
                    + "white-space:pre-wrap;word-wrap:break-word;overflow-wrap:break-word;'>")
                    .append(codeBlock).append("</pre>");
        }
        return "<html><body style='font-family:sans-serif;font-size:12px;"
                + "word-wrap:break-word;overflow-wrap:break-word;word-break:break-word;"
                + "margin:0;padding:0;'>" + body + "</body></html>";
    }

    /**
     * 将 AI 回复拆成可插入思维导图的行（跳过空行，优先使用最终回答）。
     */
    public static String[] splitIntoNodeLines(String content) {
        if (content == null || content.trim().length() == 0) {
            return new String[0];
        }
        AiCopilotResponseParser.ParsedResponse parsed = AiCopilotResponseParser.parse(content);
        String text = parsed.getFinalAnswer();
        if (text == null || text.trim().length() == 0) {
            text = content;
        }
        text = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] rawLines = text.split("\n");
        List<String> lines = new ArrayList<String>();
        for (int i = 0; i < rawLines.length; i++) {
            String trimmed = rawLines[i].trim();
            if (trimmed.length() > 0) {
                lines.add(trimmed);
            }
        }
        return lines.toArray(new String[lines.size()]);
    }

    /**
     * 将单行 Markdown 转为 Freeplane 节点可用的富文本 HTML。
     */
    public static String toNodeRichText(String markdownLine) {
        if (markdownLine == null || markdownLine.trim().length() == 0) {
            return null;
        }
        String line = markdownLine.trim();
        String body = normalizeLineContent(line);
        String escaped = escapeHtml(body);
        String formatted = formatInline(escaped);
        if (line.startsWith("### ") || line.startsWith("## ") || line.startsWith("# ")) {
            formatted = "<b>" + formatted + "</b>";
        }
        return "<html><body><p>" + formatted + "</p></body></html>";
    }

    private static String normalizeLineContent(String line) {
        if (line.startsWith("- ") || line.startsWith("* ")) {
            return line.substring(2);
        }
        if (line.matches("\\d+\\.\\s+.*")) {
            int dot = line.indexOf('.');
            return line.substring(dot + 1).trim();
        }
        if (line.startsWith("### ")) {
            return line.substring(4);
        }
        if (line.startsWith("## ")) {
            return line.substring(3);
        }
        if (line.startsWith("# ")) {
            return line.substring(2);
        }
        return line;
    }

    private static String formatInline(String line) {
        String result = line;
        result = result.replaceAll("`([^`]+)`", "<code style='background:#eee;padding:1px 3px;'>$1</code>");
        result = result.replaceAll("\\*\\*([^*]+)\\*\\*", "<b>$1</b>");
        result = result.replaceAll("\\*([^*]+)\\*", "<i>$1</i>");
        return result;
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
