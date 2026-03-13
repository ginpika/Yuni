package cc.ginpika.yuni.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PermissionRequestService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private final Map<String, CompletableFuture<PermissionResult>> pendingRequests = new ConcurrentHashMap<>();

    public PermissionRequestService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public CompletableFuture<PermissionResult> requestPermission(String requestId, String command, String reason) {
        CompletableFuture<PermissionResult> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);
        
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                    "type", "permission_request",
                    "requestId", requestId,
                    "command", command,
                    "reason", reason != null ? reason : "AI 请求执行终端命令"
            ));
            
            messagingTemplate.convertAndSend("/topic/permission", message);
            log.info("发送权限请求: requestId={}, command={}", requestId, command);
        } catch (Exception e) {
            log.error("发送权限请求失败", e);
            future.complete(new PermissionResult(false, false));
        }
        
        return future;
    }

    public void handleResponse(String requestId, boolean approved, boolean remember) {
        CompletableFuture<PermissionResult> future = pendingRequests.remove(requestId);
        if (future != null) {
            future.complete(new PermissionResult(approved, remember));
            log.info("处理权限响应: requestId={}, approved={}, remember={}", requestId, approved, remember);
        } else {
            log.warn("未找到对应的权限请求: {}", requestId);
        }
    }

    public record PermissionResult(boolean approved, boolean remember) {}
}
