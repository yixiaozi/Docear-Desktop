package org.docear.plugin.ai.prompt;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 外发 AI 前脱敏：替换密码、密钥、令牌等敏感信息。
 */
public final class AiSensitiveDataFilter {

    private static final String REDACTED = "[\u5df2\u8131\u654f]";

    private static final Pattern[] PATTERNS = new Pattern[] {
            Pattern.compile("(?i)(api[_-]?key|apikey|access[_-]?key|secret[_-]?key|client[_-]?secret)\\s*[:=]\\s*['\"]?([\\w.-]{8,})"),
            Pattern.compile("(?i)(password|passwd|pwd|token|bearer|authorization)\\s*[:=]\\s*['\"]?([^\\s'\"\\n]{4,})"),
            Pattern.compile("(?i)\u5bc6\u7801\\s*[:=\uff1a]\\s*([^\\s\\n]{2,})"),
            Pattern.compile("(?i)\u5bc6\u94a5\\s*[:=\uff1a]\\s*([^\\s\\n]{4,})"),
            Pattern.compile("(?i)bearer\\s+([a-zA-Z0-9._-]{10,})"),
            Pattern.compile("sk-[a-zA-Z0-9]{20,}"),
            Pattern.compile("AKIA[0-9A-Z]{16}"),
            Pattern.compile("eyJ[a-zA-Z0-9_-]{10,}\\.[a-zA-Z0-9._-]{10,}\\.[a-zA-Z0-9._-]{10,}"),
            Pattern.compile("-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----[\\s\\S]*?-----END (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
            Pattern.compile("(?i)(mysql|postgres|postgresql|mongodb|redis)://[^\\s:@]+:([^\\s@]+)@"),
            Pattern.compile("(?i)<password>[^<]+</password>"),
    };

    private AiSensitiveDataFilter() {
    }

    public static FilterResult filter(String text) {
        if (text == null || text.length() == 0) {
            return new FilterResult("", 0);
        }
        String result = text;
        int redactionCount = 0;
        for (int i = 0; i < PATTERNS.length; i++) {
            Matcher matcher = PATTERNS[i].matcher(result);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                redactionCount++;
                matcher.appendReplacement(sb, Matcher.quoteReplacement(REDACTED));
            }
            matcher.appendTail(sb);
            result = sb.toString();
        }
        return new FilterResult(result, redactionCount);
    }

    public static final class FilterResult {
        private final String text;
        private final int redactionCount;

        public FilterResult(String text, int redactionCount) {
            this.text = text;
            this.redactionCount = redactionCount;
        }

        public String getText() {
            return text;
        }

        public int getRedactionCount() {
            return redactionCount;
        }

        public String getNotice() {
            if (redactionCount <= 0) {
                return "";
            }
            return "\n\n[\u5b89\u5168\u63d0\u793a\uff1a\u5df2\u81ea\u52a8\u8131\u654f " + redactionCount
                    + " \u5904\u654f\u611f\u4fe1\u606f\uff08\u5bc6\u7801\u3001\u5bc6\u94a5\u3001\u4ee4\u724c\u7b49\uff09\uff0c\u672a\u53d1\u9001\u7ed9 AI\u3002]";
        }
    }
}
