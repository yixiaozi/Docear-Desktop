package org.docear.plugin.ai.backend;

/**
 * AI 流式输出回调。
 */
public interface AiChatStreamListener {

    void onChunk(String chunk);

    void onComplete(String fullText);

    void onError(String message);

    boolean isCancelled();
}
