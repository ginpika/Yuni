package cc.ginpika.yuni.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class ToolNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ToolNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyToolCall(String sessionId, String toolName, String arguments) {
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                    "type", "tool_call",
                    "sessionId", sessionId,
                    "toolName", toolName,
                    "arguments", arguments
            ));
            messagingTemplate.convertAndSend("/topic/tool", message);
            log.info("发送工具调用通知: toolName={}", toolName);
        } catch (Exception e) {
            log.error("发送工具调用通知失败", e);
        }
    }

    public void notifyToolResult(String sessionId, String toolName, String result) {
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                    "type", "tool_result",
                    "sessionId", sessionId,
                    "toolName", toolName,
                    "result", result
            ));
            messagingTemplate.convertAndSend("/topic/tool", message);
            log.info("发送工具结果通知: toolName={}", toolName);
        } catch (Exception e) {
            log.error("发送工具结果通知失败", e);
        }
    }
}
