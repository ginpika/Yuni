package cc.ginpika.yuni.tool.impl;

import cc.ginpika.yuni.service.PermissionRequestService;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RequestPermissionTool extends AbstractTool {

    @Resource
    private ToolRegistry toolRegistry;

    @Resource
    private PermissionRequestService permissionRequestService;

    @Resource
    private PermissionManager permissionManager;

    public RequestPermissionTool() {
        super("request_permission", "请求用户授权执行终端命令。当需要执行可能存在风险的命令时，使用此工具向用户请求授权，授权成功后自动执行命令。");
    }

    @PostConstruct
    public void register() {
        toolRegistry.register(this);
    }

    @Override
    protected ObjectNode buildProperties() {
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("command", createProperty("string", "需要授权执行的命令"));
        properties.set("reason", createProperty("string", "执行此命令的原因说明"));
        properties.set("timeout", createProperty("integer", "等待用户响应的超时时间（秒），默认 60 秒"));
        properties.set("remember", createProperty("boolean", "是否记住授权（加入白名单），默认 false"));
        return properties;
    }

    @Override
    protected String[] getRequiredParameters() {
        return new String[]{"command"};
    }

    @Override
    public String execute(JsonNode arguments) {
        String command = arguments.has("command") ? arguments.get("command").asText() : "";
        String reason = arguments.has("reason") ? arguments.get("reason").asText() : "AI 请求执行终端命令";
        int timeout = arguments.has("timeout") ? arguments.get("timeout").asInt() : 60;
        boolean defaultRemember = arguments.has("remember") && arguments.get("remember").asBoolean();

        if (command.isEmpty()) {
            return buildResult(false, "命令不能为空", null, null);
        }

        String requestId = UUID.randomUUID().toString();
        
        try {
            log.info("请求用户授权执行命令: {}", command);
            
            PermissionRequestService.PermissionResult result = permissionRequestService
                    .requestPermission(requestId, command, reason)
                    .get(timeout, TimeUnit.SECONDS);
            
            if (result.approved()) {
                if (result.remember() || defaultRemember) {
                    permissionManager.addToWhitelist(command);
                    log.info("命令已加入白名单: {}", command);
                }
                
                log.info("用户已授权，执行命令: {}", command);
                return executeCommand(command, timeout);
            } else {
                return buildResult(false, "用户拒绝执行命令", null, null);
            }
        } catch (Exception e) {
            log.error("请求授权失败: {}", e.getMessage());
            return buildResult(false, "请求超时或失败: " + e.getMessage(), null, null);
        }
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
        sb.append("\"success\": ").append(success);
        
        if (output != null) {
            sb.append(", \"output\": \"").append(escapeJson(output)).append("\"");
        }
        
        if (error != null) {
            sb.append(", \"error\": \"").append(escapeJson(error)).append("\"");
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
