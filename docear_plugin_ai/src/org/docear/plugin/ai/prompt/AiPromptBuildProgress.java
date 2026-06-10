package org.docear.plugin.ai.prompt;

/**
 * 构建 AI 提示词时的进度回调，便于 UI 显示当前执行到哪一步。
 */
public interface AiPromptBuildProgress {

    void onStep(int stepIndex, int stepCount, String label);
}
