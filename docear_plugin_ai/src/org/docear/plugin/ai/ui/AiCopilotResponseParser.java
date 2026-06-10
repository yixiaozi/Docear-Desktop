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
        if ("\u601d\u8003\u4e2d...".equals(normalized.trim())
                || "\u751f\u6210\u4e2d...".equals(normalized.trim())) {
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
                index++;
                continue;
            }
            if (!looksLikeAgentProgressLine(line)) {
                break;
            }
            if (thinking.length() > 0) {
                thinking.append('\n');
            }
            thinking.append(line.trim());
            index++;
        }

        while (index < lines.length && lines[index].trim().length() == 0) {
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

        if (thinkingText.length() == 0 && answerText.length() > 0) {
            ParsedResponse fallback = splitByFirstAnswerLine(normalized);
            if (fallback.hasThinking()) {
                return fallback;
            }
        }

        if (thinkingText.length() == 0) {
            return new ParsedResponse("", normalized, normalized);
        }
        if (answerText.length() == 0) {
            answerText = extractTrailingAnswer(normalized, thinkingText);
        }
        return new ParsedResponse(thinkingText, answerText, normalized);
    }

    /**
     * 从混合文本中提取末尾的最终回答（常见：多行英文日志 + 最后一行中文答案）。
     */
    private static String extractTrailingAnswer(String normalized, String thinkingText) {
        if (normalized == null || normalized.trim().length() == 0) {
            return "";
        }
        String[] lines = normalized.split("\n", -1);
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.length() == 0) {
                continue;
            }
            if (looksLikeAgentProgressLine(line)) {
                continue;
            }
            if (containsCjk(line) || !isMostlyEnglishStatus(line)) {
                return line;
            }
        }
        return "";
    }

    private static ParsedResponse splitByFirstAnswerLine(String normalized) {
        String[] lines = normalized.split("\n", -1);
        StringBuilder thinking = new StringBuilder();
        StringBuilder answer = new StringBuilder();
        boolean inAnswer = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.length() == 0) {
                if (inAnswer && answer.length() > 0) {
                    answer.append('\n');
                }
                continue;
            }
            if (!inAnswer && looksLikeAgentProgressLine(trimmed)) {
                if (thinking.length() > 0) {
                    thinking.append('\n');
                }
                thinking.append(trimmed);
                continue;
            }
            if (!inAnswer && (containsCjk(trimmed) || !isMostlyEnglishStatus(trimmed))) {
                inAnswer = true;
            }
            if (inAnswer) {
                if (answer.length() > 0) {
                    answer.append('\n');
                }
                answer.append(line);
            } else if (looksLikeAgentProgressLine(trimmed)) {
                if (thinking.length() > 0) {
                    thinking.append('\n');
                }
                thinking.append(trimmed);
            } else {
                inAnswer = true;
                if (answer.length() > 0) {
                    answer.append('\n');
                }
                answer.append(line);
            }
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
        String trimmed = normalizeLine(line.trim());
        if (trimmed.length() == 0) {
            return false;
        }
        if (containsCjk(trimmed)) {
            return false;
        }
        trimmed = stripListPrefix(trimmed);
        String lower = trimmed.toLowerCase();

        if (lower.startsWith("i'm ") || lower.startsWith("im ") || lower.startsWith("i\u2019m ")) {
            return true;
        }
        if (lower.startsWith("i am ") || lower.startsWith("i found ") || lower.startsWith("i have ")
                || lower.startsWith("i will ") || lower.startsWith("i need ") || lower.startsWith("i should ")
                || lower.startsWith("i can ") || lower.startsWith("i just ") || lower.startsWith("i now ")
                || lower.startsWith("i've ") || lower.startsWith("i'll ")) {
            return true;
        }
        if (lower.startsWith("the prompt ") || lower.startsWith("the file ") || lower.startsWith("the task ")
                || lower.startsWith("the map ") || lower.startsWith("the question ")
                || lower.startsWith("the user ") || lower.startsWith("the request ")
                || lower.startsWith("the content ") || lower.startsWith("the data ")
                || lower.startsWith("the attached ") || lower.startsWith("the generated ")
                || lower.startsWith("the injected ") || lower.startsWith("the local ")) {
            return true;
        }
        if (lower.startsWith("now i ") || lower.startsWith("now i'm ") || lower.startsWith("now im ")
                || lower.startsWith("next i ") || lower.startsWith("next i'm ")) {
            return true;
        }
        if (lower.startsWith("let me ") || lower.startsWith("reading ") || lower.startsWith("opening ")
                || lower.startsWith("extracting ") || lower.startsWith("searching ") || lower.startsWith("checking ")
                || lower.startsWith("analyzing ") || lower.startsWith("looking ") || lower.startsWith("processing ")
                || lower.startsWith("preparing ") || lower.startsWith("fetching ") || lower.startsWith("loading ")
                || lower.startsWith("tracing ") || lower.startsWith("locating ") || lower.startsWith("implementing ")) {
            return true;
        }
        if (lower.contains("injected context") || lower.contains("prompt file")
                || lower.contains("requested change") || lower.contains("implement it directly")
                || lower.contains("relevant code path") || lower.contains("docear distribution")
                || lower.contains("generated prompt") || lower.contains("carry it through")
                || lower.contains("end to end") || lower.contains("actionable task")) {
            return true;
        }
        return isMostlyEnglishStatus(trimmed) && containsAgentVerbPhrase(lower);
    }

    private static boolean containsAgentVerbPhrase(String lower) {
        return lower.contains(" i'm ") || lower.contains(" i am ") || lower.contains(" i found ")
                || lower.contains(" i have ") || lower.contains(" i will ") || lower.contains(" i need ")
                || lower.contains(" i can ") || lower.contains(" i'll ") || lower.contains(" i've ")
                || lower.contains("next i'm") || lower.contains("now i'm") || lower.contains("then i'll")
                || lower.contains("so i can") || lower.contains("and i'll");
    }

    private static boolean isMostlyEnglishStatus(String text) {
        int letters = 0;
        int cjk = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                letters++;
            } else if (containsCjk(String.valueOf(c))) {
                cjk++;
            }
        }
        return letters > 0 && cjk == 0;
    }

    private static String normalizeLine(String line) {
        return line.replace('\u2019', '\'').replace('\u2018', '\'').replace('\u0060', '\'');
    }

    private static String stripListPrefix(String line) {
        if (line.startsWith("- ") || line.startsWith("* ") || line.startsWith("• ")) {
            return line.substring(2).trim();
        }
        if (line.matches("\\d+\\.\\s+.*")) {
            int dot = line.indexOf('.');
            return line.substring(dot + 1).trim();
        }
        return line;
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
