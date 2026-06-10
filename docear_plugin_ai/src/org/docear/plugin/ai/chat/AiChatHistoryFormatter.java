package org.docear.plugin.ai.chat;

import java.util.List;

import org.docear.plugin.ai.DocearAiConfig;

/**
 * 将聊天历史格式化为可嵌入提示词的上下文文本。
 */
public final class AiChatHistoryFormatter {

    private AiChatHistoryFormatter() {
    }

    public static String formatForContext(AiChatSession session, boolean excludeLastUserMessage) {
        if (session == null || session.getMessages().isEmpty()) {
            return "\uff08\u65e0\u5386\u53f2\u5bf9\u8bdd\uff09";
        }

        DocearAiConfig config = new DocearAiConfig();
        int maxTurns = config.getMaxContextTurns();
        int maxChars = config.getMaxContextChars();

        List<AiChatMessage> messages = session.getMessages();
        int endIndex = messages.size();
        if (excludeLastUserMessage && endIndex > 0) {
            AiChatMessage last = messages.get(endIndex - 1);
            if (last.getRole() == AiChatMessage.Role.USER) {
                endIndex--;
            }
        }
        if (endIndex <= 0) {
            return "\uff08\u65e0\u5386\u53f2\u5bf9\u8bdd\uff09";
        }

        int startIndex = 0;
        int pairCount = 0;
        for (int i = endIndex - 1; i >= 0; i--) {
            if (messages.get(i).getRole() == AiChatMessage.Role.USER) {
                pairCount++;
                if (pairCount > maxTurns) {
                    startIndex = i + 1;
                    break;
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < endIndex; i++) {
            AiChatMessage message = messages.get(i);
            if (message.getRole() == AiChatMessage.Role.USER) {
                sb.append("\u7528\u6237: ").append(message.getContent()).append('\n');
            } else {
                sb.append("AI: ").append(message.getContent()).append('\n');
            }
        }

        String history = sb.toString().trim();
        if (history.length() > maxChars) {
            history = history.substring(history.length() - maxChars);
            history = "[\u5386\u53f2\u5df2\u622a\u65ad]\n" + history;
        }
        return history.length() > 0 ? history : "\uff08\u65e0\u5386\u53f2\u5bf9\u8bdd\uff09";
    }
}
