package org.docear.plugin.ai.backend;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.freeplane.core.util.LogUtils;

/**
 * 基于 GitHub Copilot CLI 的 AI 后端实现。
 * 通过调用系统中的 `copilot` 命令行工具来实现 AI 功能。
 */
public class CopilotCliBackend implements AiBackend {

    private static final String COPILOT_COMMAND = "copilot";
    private static final long PROCESS_TIMEOUT_SECONDS = 120;

    @Override
    public List<String> generateSubNodes(String prompt, int count) {
        List<String> subNodes = new ArrayList<>();

        if (!isAvailable()) {
            LogUtils.warn("Copilot CLI is not available. Please install it via 'npm install -g @github/copilot'.");
            return subNodes;
        }

        try {
            // 构造提示词，明确要求返回结构化的子节点列表
            String fullPrompt = String.format(
                "请为以下主题生成 %d 个子主题（仅返回标题列表，每行一个，不要解释）：\n%s",
                count, prompt
            );

            ProcessBuilder pb = new ProcessBuilder(COPILOT_COMMAND, "-p", fullPrompt, "-s");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                LogUtils.warn("Copilot CLI process timed out.");
                return subNodes;
            }

            // 解析输出（简单按行分割，过滤空行）
            String[] lines = output.toString().split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && subNodes.size() < count) {
                    // 去除可能的编号或 bullet
                    trimmed = trimmed.replaceAll("^[-*\\d.]+\\s*", "");
                    subNodes.add(trimmed);
                }
            }

        } catch (Exception e) {
            LogUtils.severe("Error calling Copilot CLI: " + e.getMessage());
        }

        return subNodes;
    }

    @Override
    public boolean isAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(COPILOT_COMMAND, "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
