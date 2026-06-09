package org.docear.plugin.ai.backend;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.freeplane.core.util.LogUtils;

/**
 * 基于 GitHub Copilot CLI 的 AI 后端实现。
 * 通过调用系统中的 `copilot` 命令行工具来实现 AI 功能。
 * 
 * 注意：本类使用 Java 1.6 兼容语法编写。
 */
public class CopilotCliBackend implements AiBackend {

    private static final String COPILOT_COMMAND = "copilot";
    private static final long PROCESS_TIMEOUT_MILLIS = 120000; // 120 秒

    /**
     * 判断当前操作系统是否为 Windows。
     */
    private boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("windows");
    }

    @Override
    public List<String> generateSubNodes(String prompt, int count) {
        List<String> subNodes = new ArrayList<String>();

        if (!isAvailable()) {
            LogUtils.warn("Copilot CLI is not available. Please install it via 'npm install -g @github/copilot'.");
            return subNodes;
        }

        Process process = null;
        BufferedReader reader = null;
        try {
            // 构造提示词，明确要求返回结构化的子节点列表
            String fullPrompt = "请为以下主题生成 " + count + " 个子主题（仅返回标题列表，每行一个，不要解释）：\n" + prompt;

            ProcessBuilder pb;
            if (isWindows()) {
                // Windows 下通过 PowerShell 调用（因为 copilot 只在 PowerShell 的 PATH 中）
                String escaped = fullPrompt.replace("\"", "\\\"");
                String psCommand = "copilot -p \"" + escaped + "\" -s";
                pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-Command", psCommand);
            } else {
                pb = new ProcessBuilder(COPILOT_COMMAND, "-p", fullPrompt, "-s");
            }
            pb.redirectErrorStream(true);
            process = pb.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // 等待进程结束（带超时）
            long startTime = System.currentTimeMillis();
            boolean finished = false;
            while ((System.currentTimeMillis() - startTime) < PROCESS_TIMEOUT_MILLIS) {
                try {
                    process.exitValue();
                    finished = true;
                    break;
                } catch (IllegalThreadStateException e) {
                    Thread.sleep(500);
                }
            }

            if (!finished) {
                process.destroy();
                LogUtils.warn("Copilot CLI process timed out.");
                return subNodes;
            }

            // 解析输出（简单按行分割，过滤空行）
            String[] lines = output.toString().split("\n");
            for (int i = 0; i < lines.length; i++) {
                String trimmed = lines[i].trim();
                if (trimmed.length() > 0 && subNodes.size() < count) {
                    // 去除可能的编号或 bullet
                    trimmed = trimmed.replaceAll("^[-*\\d.]+\\s*", "");
                    subNodes.add(trimmed);
                }
            }

        } catch (Exception e) {
            LogUtils.severe("Error calling Copilot CLI: " + e.getMessage());
        } finally {
            // 手动关闭资源（Java 1.6 兼容）
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
            if (process != null) {
                process.destroy();
            }
        }

        return subNodes;
    }

    @Override
    public boolean isAvailable() {
        // 在 Windows 上，只要 PowerShell 能找到 copilot，就认为可用
        // 因为用户确认在 PowerShell 中可以正常使用正确的 copilot
        if (isWindows()) {
            return true;
        }

        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(COPILOT_COMMAND, "--help");
            process = pb.start();

            long startTime = System.currentTimeMillis();
            boolean finished = false;
            while ((System.currentTimeMillis() - startTime) < 10000) {
                try {
                    process.exitValue();
                    finished = true;
                    break;
                } catch (IllegalThreadStateException e) {
                    Thread.sleep(200);
                }
            }

            if (!finished) {
                process.destroy();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
}
