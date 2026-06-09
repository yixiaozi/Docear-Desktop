package org.docear.plugin.ai.backend;

import java.util.List;

/**
 * AI 后端接口。所有具体的 AI 实现（Copilot CLI、OpenAI、Ollama 等）都必须实现此接口。
 * 这样可以方便未来切换模型，而不需要修改上层业务代码。
 */
public interface AiBackend {

    /**
     * 根据提示词生成子节点标题列表。
     * @param prompt 用户输入的提示词（通常包含当前节点的内容）
     * @param count 期望生成的子节点数量
     * @return 子节点标题列表
     */
    List<String> generateSubNodes(String prompt, int count);

    /**
     * 发送聊天消息并返回 AI 回复。
     */
    String chat(String message);

    /**
     * 判断当前后端是否可用（例如 Copilot CLI 是否已安装）。
     */
    boolean isAvailable();
}
