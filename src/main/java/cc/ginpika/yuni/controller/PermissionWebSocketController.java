package cc.ginpika.yuni.controller;

import cc.ginpika.yuni.service.PermissionRequestService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
public class PermissionWebSocketController {

    @Resource
    private PermissionRequestService permissionRequestService;

    @MessageMapping("/permission/response")
    public void handlePermissionResponse(@Payload Map<String, Object> payload) {
        String requestId = (String) payload.get("requestId");
        Boolean approved = (Boolean) payload.get("approved");
        Boolean remember = (Boolean) payload.get("remember");
        
        log.info("收到权限响应: requestId={}, approved={}, remember={}", requestId, approved, remember);
        
        if (requestId != null && approved != null) {
            permissionRequestService.handleResponse(requestId, approved, remember != null && remember);
        }
    }
}
