package cc.ginpika.yuni.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ToolExecutor {
    @Resource
    private ToolRegistry toolRegistry;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ToolResult execute(String toolName, JsonNode arguments) {
        Tool tool = toolRegistry.getTool(toolName);
        if (tool == null) {
            log.error("工具不存在: {}", toolName);
            return ToolResult.error("工具不存在: " + toolName);
        }
        
        try {
            log.info("执行工具: {} 参数: {}", toolName, arguments);
            String result = tool.execute(arguments);
            log.info("工具执行结果: {}", result);
            return ToolResult.success(result);
        } catch (Exception e) {
            log.error("工具执行失败: {} - {}", toolName, e.getMessage(), e);
            return ToolResult.error("工具执行失败: " + e.getMessage());
        }
    }

    public ToolResult executeFromJson(JsonNode toolCall) {
        try {
            String toolName = toolCall.at("/function/name").asText();
            JsonNode arguments = toolCall.at("/function/arguments");
            
            if (toolName == null || toolName.isEmpty()) {
                return ToolResult.error("工具调用缺少 name 字段");
            }
            
            if (arguments.isTextual()) {
                arguments = objectMapper.readTree(arguments.asText());
            }
            
            return execute(toolName, arguments);
        } catch (Exception e) {
            log.error("解析工具调用失败: {}", e.getMessage(), e);
            return ToolResult.error("解析工具调用失败: " + e.getMessage());
        }
    }

    public ObjectNode buildToolResultMessage(String toolCallId, ToolResult result) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", "tool");
        message.put("tool_call_id", toolCallId);
        message.put("content", result.toJson());
        return message;
    }

    public static class ToolResult {
        private final boolean success;
        private final String result;
        private final String error;

        private ToolResult(boolean success, String result, String error) {
            this.success = success;
            this.result = result;
            this.error = error;
        }

        public static ToolResult success(String result) {
            return new ToolResult(true, result, null);
        }

        public static ToolResult error(String error) {
            return new ToolResult(false, null, error);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getResult() {
            return result;
        }

        public String getError() {
            return error;
        }

        public String toJson() {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode node = mapper.createObjectNode();
            node.put("success", success);
            if (success) {
                node.put("result", result);
            } else {
                node.put("error", error);
            }
            return node.toString();
        }
    }
}
