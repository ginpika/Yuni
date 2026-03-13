package cc.ginpika.yuni.tool.impl;

import cc.ginpika.yuni.tool.AbstractTool;
import cc.ginpika.yuni.tool.PermissionManager;
import cc.ginpika.yuni.tool.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TerminalTool extends AbstractTool {

    @Resource
    private ToolRegistry toolRegistry;

    @Resource
    private PermissionManager permissionManager;

    private static final List<String> SAFE_COMMANDS = Arrays.asList(
            "ls",
            "pwd",
            "whoami",
            "date",
            "echo",
            "cat ",
            "head ",
            "tail ",
            "grep ",
            "find ",
            "wc ",
            "sort ",
            "uniq ",
            "diff ",
            "tree",
            "du ",
            "df -h",
            "uname",
            "hostname",
            "uptime",
            "top -l 1",
            "ps aux",
            "which ",
            "type ",
            "alias",
            "env",
            "printenv",
            "git status",
            "git log",
            "git branch",
            "git diff",
            "git remote -v"
    );

    public TerminalTool() {
        super("terminal", "在 macOS 终端执行 shell 命令，返回命令执行结果。如果命令不在白名单中，需要用户授权。");
    }

    @PostConstruct
    public void register() {
        toolRegistry.register(this);
    }

    @Override
    protected ObjectNode buildProperties() {
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("command", createProperty("string", "要执行的 shell 命令"));
        properties.set("timeout", createProperty("integer", "超时时间（秒），默认 30 秒"));
        properties.set("requestId", createProperty("string", "权限请求ID（如果之前返回了 need_permission，使用此ID授权后再次调用）"));
        return properties;
    }

    @Override
    protected String[] getRequiredParameters() {
        return new String[]{"command"};
    }

    @Override
    public String execute(JsonNode arguments) {
        String command = arguments.has("command") ? arguments.get("command").asText() : "";
        int timeout = arguments.has("timeout") ? arguments.get("timeout").asInt() : 30;
        String requestId = arguments.has("requestId") ? arguments.get("requestId").asText() : null;

        if (command.isEmpty()) {
            return buildResult(false, "命令不能为空", null, null);
        }

        if (isSafeCommand(command)) {
            log.info("执行安全命令: {}", command);
            return executeCommand(command, timeout);
        }

        if (permissionManager.isWhitelisted(command)) {
            log.info("执行白名单命令: {}", command);
            return executeCommand(command, timeout);
        }

        if (requestId != null && permissionManager.isApproved(requestId)) {
            log.info("执行已授权命令: {}", command);
            permissionManager.removeRequest(requestId);
            return executeCommand(command, timeout);
        }

        if (requestId != null) {
            PermissionManager.PermissionStatus status = permissionManager.checkStatus(requestId);
            if (status == PermissionManager.PermissionStatus.REJECTED) {
                permissionManager.removeRequest(requestId);
                return buildResult(false, "用户拒绝了该命令的执行请求", null, null);
            }
            return buildResult(false, "权限请求仍在等待中，请等待用户批准", null, null);
        }

        String newRequestId = permissionManager.requestPermission(command);
        return String.format(
                "{\"need_permission\": true, \"request_id\": \"%s\", \"command\": \"%s\", \"message\": \"该命令需要用户授权才能执行，请询问用户是否允许执行此命令。如果用户同意，请使用 request_id 再次调用此工具。\"}",
                newRequestId,
                escapeJson(command)
        );
    }

    private boolean isSafeCommand(String command) {
        String trimmed = command.trim();
        for (String safe : SAFE_COMMANDS) {
            if (trimmed.startsWith(safe)) {
                return true;
            }
        }
        return false;
    }

    private String executeCommand(String command, int timeout) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("/bin/zsh", "-c", command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return buildResult(false, String.format("命令执行超时（%d秒）", timeout), null, null);
            }

            int exitCode = process.exitValue();
            String result = output.toString().trim();

            if (result.isEmpty()) {
                result = "(无输出)";
            }

            if (exitCode == 0) {
                return buildResult(true, result, null, exitCode);
            } else {
                return buildResult(false, result, null, exitCode);
            }

        } catch (Exception e) {
            log.error("执行命令失败: {}", e.getMessage(), e);
            return buildResult(false, e.getMessage(), null, null);
        }
    }

    private String buildResult(Boolean success, String output, String error, Integer exitCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\": ").append(success).append(", ");
        
        if (output != null) {
            sb.append("\"output\": \"").append(escapeJson(output)).append("\"");
        }
        
        if (error != null) {
            if (output != null) sb.append(", ");
            sb.append("\"error\": \"").append(escapeJson(error)).append("\"");
        }
        
        if (exitCode != null) {
            sb.append(", \"exit_code\": ").append(exitCode);
        }
        
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
