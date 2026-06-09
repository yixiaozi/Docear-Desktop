package org.docear.plugin.ai.backend;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.freeplane.core.util.LogUtils;

/**
 * 基于 GitHub Copilot CLI 的 AI 后端实现。
 * Windows 下通过 PowerShell 调用 copilot，提示词经 UTF-8 临时文件传递以避免编码问题。
 *
 * 注意：本类使用 Java 1.6 兼容语法编写。
 */
public class CopilotCliBackend implements AiBackend {

    private static final String COPILOT_COMMAND = "copilot";
    private static final long PROCESS_TIMEOUT_MILLIS = 120000; // 120 秒

    private String resolvedCopilotPath;

    private boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("windows");
    }

    @Override
    public List<String> generateSubNodes(String prompt, int count) {
        String fullPrompt = "请为以下主题生成 " + count + " 个子主题（仅返回标题列表，每行一个，不要解释）：\n" + prompt;
        String output = runCopilot(fullPrompt);
        return parseSubNodes(output, count);
    }

    @Override
    public String chat(String message) {
        if (message == null || message.trim().length() == 0) {
            return "";
        }
        return runCopilot(message.trim());
    }

    @Override
    public boolean isAvailable() {
        if (isWindows()) {
            return resolveCopilotExecutable() != null;
        }
        return testCopilotDirect(COPILOT_COMMAND);
    }

    private String runCopilot(String prompt) {
        String copilotPath = resolveCopilotExecutable();
        if (copilotPath == null) {
            LogUtils.warn("Copilot CLI is not available. Please install it via 'npm install -g @github/copilot'.");
            return "";
        }

        File promptFile = null;
        Process process = null;
        BufferedReader reader = null;
        try {
            promptFile = writePromptToTempFile(prompt);
            ProcessBuilder pb = new ProcessBuilder(buildPowerShellCommand(promptFile, copilotPath));
            pb.redirectErrorStream(true);
            enrichPath(pb.environment());
            process = pb.start();

            StringBuilder output = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            if (!waitForProcess(process, PROCESS_TIMEOUT_MILLIS)) {
                process.destroy();
                LogUtils.warn("Copilot CLI process timed out.");
                return "";
            }

            if (process.exitValue() != 0) {
                LogUtils.warn("Copilot CLI exited with code: " + process.exitValue() + ", output: " + output);
            }

            String result = output.toString().trim();
            if (looksLikeCommandEcho(result)) {
                LogUtils.warn("Copilot CLI returned command echo instead of content: " + result);
                return "";
            }
            return result;
        } catch (Exception e) {
            LogUtils.severe("Error calling Copilot CLI: " + e.getMessage());
            return "";
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
            if (process != null) {
                process.destroy();
            }
            if (promptFile != null) {
                promptFile.delete();
            }
        }
    }

    private File writePromptToTempFile(String prompt) throws Exception {
        File file = File.createTempFile("docear_ai_prompt_", ".txt");
        FileOutputStream fos = new FileOutputStream(file);
        try {
            fos.write(0xEF);
            fos.write(0xBB);
            fos.write(0xBF);
            OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
            writer.write(prompt);
            writer.flush();
            writer.close();
        } finally {
            fos.close();
        }
        return file;
    }

    private List<String> buildPowerShellCommand(File promptFile, String copilotPath) {
        String promptPath = escapePowerShellSingleQuoted(promptFile.getAbsolutePath());
        String copilotLiteral = escapePowerShellSingleQuoted(copilotPath);
        String script = "[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false); "
                + "$OutputEncoding = [Console]::OutputEncoding; "
                + "chcp 65001 | Out-Null; "
                + "$prompt = Get-Content -LiteralPath '" + promptPath + "' -Raw -Encoding UTF8; "
                + "& '" + copilotLiteral + "' -p $prompt -s";

        List<String> command = new ArrayList<String>();
        command.add("powershell.exe");
        command.add("-NoProfile");
        command.add("-ExecutionPolicy");
        command.add("Bypass");
        command.add("-Command");
        command.add(script);
        return command;
    }

    private String escapePowerShellSingleQuoted(String value) {
        return value.replace("'", "''");
    }

    /**
     * 解析 copilot 可执行文件路径。
     * GUI 应用启动时通常拿不到用户级 PATH，因此需要主动搜索常见安装位置。
     */
    private String resolveCopilotExecutable() {
        if (resolvedCopilotPath != null) {
            return resolvedCopilotPath;
        }

        List<String> candidates = new ArrayList<String>();
        String appData = System.getenv("APPDATA");
        if (appData != null) {
            candidates.add(appData + "\\npm\\copilot.cmd");
            candidates.add(appData + "\\npm\\copilot.ps1");
        }
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null) {
            candidates.add(localAppData + "\\npm\\copilot.cmd");
            candidates.add(localAppData + "\\npm\\copilot.ps1");
            candidates.add(localAppData + "\\Microsoft\\WinGet\\Links\\copilot.exe");
        }
        String programFiles = System.getenv("ProgramFiles");
        if (programFiles != null) {
            candidates.add(programFiles + "\\nodejs\\copilot.cmd");
        }
        String programFilesX86 = System.getenv("ProgramFiles(x86)");
        if (programFilesX86 != null) {
            candidates.add(programFilesX86 + "\\nodejs\\copilot.cmd");
        }

        for (int i = 0; i < candidates.size(); i++) {
            File file = new File(candidates.get(i));
            if (file.exists()) {
                resolvedCopilotPath = file.getAbsolutePath();
                LogUtils.info("Resolved Copilot CLI path: " + resolvedCopilotPath);
                return resolvedCopilotPath;
            }
        }

        String[] pathEntries = getEffectivePath().split(";");
        for (int i = 0; i < pathEntries.length; i++) {
            String entry = pathEntries[i].trim();
            if (entry.length() == 0) {
                continue;
            }
            String[] names = new String[] { "copilot.cmd", "copilot.ps1", "copilot.exe", "copilot" };
            for (int j = 0; j < names.length; j++) {
                File file = new File(entry, names[j]);
                if (file.exists()) {
                    resolvedCopilotPath = file.getAbsolutePath();
                    LogUtils.info("Resolved Copilot CLI path from PATH: " + resolvedCopilotPath);
                    return resolvedCopilotPath;
                }
            }
        }

        String whereResult = resolveViaWhereCommand();
        if (whereResult != null) {
            resolvedCopilotPath = whereResult;
            LogUtils.info("Resolved Copilot CLI path via where: " + resolvedCopilotPath);
            return resolvedCopilotPath;
        }

        return null;
    }

    private String resolveViaWhereCommand() {
        Process process = null;
        BufferedReader reader = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "where", "copilot");
            pb.redirectErrorStream(true);
            enrichPath(pb.environment());
            process = pb.start();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.length() == 0) {
                    continue;
                }
                File file = new File(trimmed);
                if (file.exists()) {
                    if (!waitForProcess(process, 5000)) {
                        process.destroy();
                    }
                    return file.getAbsolutePath();
                }
            }
            if (!waitForProcess(process, 5000)) {
                process.destroy();
            }
        } catch (Exception e) {
            LogUtils.warn("Failed to resolve copilot via where: " + e.getMessage());
        } finally {
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
        return null;
    }

    private String getEffectivePath() {
        StringBuilder path = new StringBuilder();
        appendPathValue(path, System.getenv("PATH"));
        appendPathValue(path, queryRegistryPath("HKCU\\Environment"));
        appendPathValue(path, queryRegistryPath("HKLM\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment"));

        String appData = System.getenv("APPDATA");
        if (appData != null) {
            appendPathValue(path, appData + "\\npm");
        }
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null) {
            appendPathValue(path, localAppData + "\\Microsoft\\WinGet\\Links");
        }
        String programFiles = System.getenv("ProgramFiles");
        if (programFiles != null) {
            appendPathValue(path, programFiles + "\\nodejs");
        }
        return path.toString();
    }

    private void appendPathValue(StringBuilder target, String value) {
        if (value == null || value.trim().length() == 0) {
            return;
        }
        if (target.length() > 0) {
            target.append(";");
        }
        target.append(value.trim());
    }

    private String queryRegistryPath(String registryKey) {
        Process process = null;
        BufferedReader reader = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("reg", "query", registryKey, "/v", "Path");
            pb.redirectErrorStream(true);
            process = pb.start();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("Path") && trimmed.contains("REG_")) {
                    int index = trimmed.indexOf("REG_");
                    String tail = trimmed.substring(index);
                    int space = tail.indexOf("    ");
                    if (space >= 0 && space + 4 < tail.length()) {
                        return tail.substring(space + 4).trim();
                    }
                }
            }
            if (!waitForProcess(process, 5000)) {
                process.destroy();
            }
        } catch (Exception e) {
            LogUtils.warn("Failed to read registry PATH from " + registryKey + ": " + e.getMessage());
        } finally {
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
        return null;
    }

    private void enrichPath(Map<String, String> environment) {
        String effectivePath = getEffectivePath();
        if (effectivePath.length() > 0) {
            environment.put("PATH", effectivePath);
        }
    }

    private boolean waitForProcess(Process process, long timeoutMillis) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - startTime) < timeoutMillis) {
            try {
                process.exitValue();
                return true;
            } catch (IllegalThreadStateException e) {
                Thread.sleep(200);
            }
        }
        return false;
    }

    private List<String> parseSubNodes(String output, int count) {
        List<String> subNodes = new ArrayList<String>();
        if (output == null || output.trim().length() == 0) {
            return subNodes;
        }

        String[] lines = output.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (trimmed.length() == 0 || looksLikeCommandEcho(trimmed)) {
                continue;
            }
            trimmed = trimmed.replaceAll("^[-*\\d.]+\\s*", "");
            if (trimmed.length() > 0 && subNodes.size() < count) {
                subNodes.add(trimmed);
            }
        }
        return subNodes;
    }

    private boolean looksLikeCommandEcho(String text) {
        if (text == null) {
            return false;
        }
        String lower = text.toLowerCase();
        return lower.contains("copilot -p")
                || lower.contains("powershell.exe")
                || lower.contains("get-content -literalpath")
                || lower.contains("objectnotfound")
                || lower.contains("commandnotfoundexception");
    }

    private boolean testCopilotDirect(String command) {
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(command, "--version");
            pb.redirectErrorStream(true);
            process = pb.start();
            if (!waitForProcess(process, 10000)) {
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
