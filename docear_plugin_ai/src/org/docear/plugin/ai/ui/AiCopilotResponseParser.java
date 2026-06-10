package org.docear.plugin.ai.ui;

/**
 * 将 Copilot CLI 输出拆分为「思考过程日志」与「最终回答」。
 * Copilot 代理模式会在回答前输出英文进度行（如 "I'm opening the task file..."）。
 */
public final class AiCopilotResponseParser {

    public static final class ParsedResponse {
        private final String thinkingLog;
        private final String finalAnswer;
        private final String raw;

        ParsedResponse(String thinkingLog, String finalAnswer, String raw) {
            this.thinkingLog = thinkingLog != null ? thinkingLog : "";
            this.finalAnswer = finalAnswer != null ? finalAnswer : "";
            this.raw = raw != null ? raw : "";
        }

        public String getThinkingLog() {
            return thinkingLog;
        }

        public String getFinalAnswer() {
            return finalAnswer;
        }

        public String getRaw() {
            return raw;
        }

        public boolean hasThinking() {
            return thinkingLog.trim().length() > 0;
        }

        public int getThinkingLineCount() {
            if (!hasThinking()) {
                return 0;
            }
            String[] lines = thinkingLog.split("\n");
            int count = 0;
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].trim().length() > 0) {
                    count++;
                }
            }
            return count;
        }
    }

    private AiCopilotResponseParser() {
    }

    public static ParsedResponse parse(String text) {
        if (text == null || text.trim().length() == 0) {
            return new ParsedResponse("", "", text != null ? text : "");
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        if ("\u601d\u8003\u4e2d...".equals(normalized.trim())) {
            return new ParsedResponse("", normalized, normalized);
        }

        String[] lines = normalized.split("\n", -1);
        StringBuilder thinking = new StringBuilder();
        int index = 0;
        while (index < lines.length) {
            String line = lines[index];
            if (line.trim().length() == 0) {
                if (thinking.length() == 0) {
                    index++;
                    continue;
                }
                break;
            }
            if (!looksLikeAgentProgressLine(line)) {
                break;
            }
            if (thinking.length() > 0) {
                thinking.append('\n');
            }
            thinking.append(line);
            index++;
        }

        StringBuilder answer = new StringBuilder();
        while (index < lines.length) {
            if (answer.length() > 0) {
                answer.append('\n');
            }
            answer.append(lines[index]);
            index++;
        }

        String thinkingText = thinking.toString().trim();
        String answerText = answer.toString().trim();
        if (thinkingText.length() == 0) {
            return new ParsedResponse("", normalized, normalized);
        }
        return new ParsedResponse(thinkingText, answerText, normalized);
    }

    private static boolean looksLikeAgentProgressLine(String line) {
        if (line == null) {
            return false;
        }
        String trimmed = line.trim();
        if (trimmed.length() == 0) {
            return false;
        }
        if (containsCjk(trimmed)) {
            return false;
        }
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("i'm ") || lower.startsWith("im ")) {
            return true;
        }
        if (lower.startsWith("i am ")) {
            return true;
        }
        if (lower.startsWith("i found ") || lower.startsWith("i have ") || lower.startsWith("i will ")
                || lower.startsWith("i need ") || lower.startsWith("i should ") || lower.startsWith("i can ")
                || lower.startsWith("i just ") || lower.startsWith("i now ") || lower.startsWith("i've ")
                || lower.startsWith("i'll ")) {
            return true;
        }
        if (lower.startsWith("the prompt ") || lower.startsWith("the file ") || lower.startsWith("the task ")
                || lower.startsWith("the map ") || lower.startsWith("the question ")
                || lower.startsWith("the user ") || lower.startsWith("the request ")
                || lower.startsWith("the content ") || lower.startsWith("the data ")) {
            return true;
        }
        if (lower.startsWith("now i ") || lower.startsWith("now i'm ") || lower.startsWith("now im ")) {
            return true;
        }
        if (lower.startsWith("let me ") || lower.startsWith("reading ") || lower.startsWith("opening ")
                || lower.startsWith("extracting ") || lower.startsWith("searching ") || lower.startsWith("checking ")
                || lower.startsWith("analyzing ") || lower.startsWith("looking ") || lower.startsWith("processing ")
                || lower.startsWith("preparing ") || lower.startsWith("fetching ") || lower.startsWith("loading ")) {
            return true;
        }
        return false;
    }

    private static boolean containsCjk(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF) {
                return true;
            }
            if (c >= 0x3400 && c <= 0x4DBF) {
                return true;
            }
            if (c >= 0xF900 && c <= 0xFAFF) {
                return true;
            }
        }
        return false;
    }
}
