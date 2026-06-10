package org.docear.plugin.ai.ui;

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
                    body.append("<pre style='background:#f4f4f4;padding:6px;border-radius:4px;'>")
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
                body.append("<p>").append(formatted).append("</p>");
            }
        }
        if (inCode && codeBlock.length() > 0) {
            body.append("<pre style='background:#f4f4f4;padding:6px;border-radius:4px;'>")
                    .append(codeBlock).append("</pre>");
        }
        return "<html><body style='font-family:sans-serif;font-size:12px;'>" + body + "</body></html>";
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
