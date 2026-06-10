package org.docear.plugin.ai.prompt;

/**
 * 按 Freeplane/Docear 惯例将非 ASCII 字符编码为 &#x...;，避免 Windows GBK 读盘乱码。
 */
public final class AiXmlEncoding {

    private AiXmlEncoding() {
    }

    public static String encodeXmlText(String value) {
        if (value == null || value.length() == 0) {
            return "";
        }
        StringBuilder result = null;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            String encoded = encodeChar(c, false);
            if (encoded == null) {
                if (result != null) {
                    result.append(c);
                }
            } else {
                if (result == null) {
                    result = new StringBuilder((int) (value.length() * 1.4));
                    result.append(value.substring(0, i));
                }
                result.append(encoded);
            }
        }
        return result != null ? result.toString() : value;
    }

    public static String encodeXmlAttribute(String value) {
        if (value == null || value.length() == 0) {
            return "";
        }
        StringBuilder result = null;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            String encoded = encodeChar(c, true);
            if (encoded == null) {
                if (result != null) {
                    result.append(c);
                }
            } else {
                if (result == null) {
                    result = new StringBuilder((int) (value.length() * 1.4));
                    result.append(value.substring(0, i));
                }
                result.append(encoded);
            }
        }
        return result != null ? result.toString() : value;
    }

    private static String encodeChar(char c, boolean attributeValue) {
        if (c > 0x7E) {
            return "&#x" + Integer.toString(c, 16) + ";";
        }
        switch (c) {
            case '<':
                return "&lt;";
            case '>':
                return "&gt;";
            case '&':
                return "&amp;";
            case '\'':
                return "&apos;";
            case '"':
                return "&quot;";
            case '\n':
                return attributeValue ? "&#xa;" : null;
            default:
                if (c < ' ') {
                    return "&#x" + Integer.toString(c, 16) + ";";
                }
                return null;
        }
    }
}
