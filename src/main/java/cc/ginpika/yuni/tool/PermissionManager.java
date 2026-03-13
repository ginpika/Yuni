package cc.ginpika.yuni.tool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class PermissionManager {

    @Data
    @AllArgsConstructor
    public static class PermissionRequest {
        private String requestId;
        private String command;
        private long timestamp;
        private PermissionStatus status;
    }

    public enum PermissionStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    private final Map<String, PermissionRequest> pendingRequests = new ConcurrentHashMap<>();
    private final Map<String, Boolean> whitelist = new ConcurrentHashMap<>();

    public String requestPermission(String command) {
        String requestId = "perm_" + System.currentTimeMillis() + "_" + Math.abs(command.hashCode());
        PermissionRequest request = new PermissionRequest(
                requestId, 
                command, 
                System.currentTimeMillis(), 
                PermissionStatus.PENDING
        );
        pendingRequests.put(requestId, request);
        log.info("权限请求: {} -> {}", command, requestId);
        return requestId;
    }

    public PermissionRequest getRequest(String requestId) {
        return pendingRequests.get(requestId);
    }

    public List<PermissionRequest> getPendingRequests() {
        List<PermissionRequest> pending = new ArrayList<>();
        for (PermissionRequest request : pendingRequests.values()) {
            if (request.getStatus() == PermissionStatus.PENDING) {
                pending.add(request);
            }
        }
        return pending;
    }

    public void approve(String requestId) {
        PermissionRequest request = pendingRequests.get(requestId);
        if (request != null) {
            request.setStatus(PermissionStatus.APPROVED);
            log.info("权限已批准: {}", requestId);
        }
    }

    public void reject(String requestId) {
        PermissionRequest request = pendingRequests.get(requestId);
        if (request != null) {
            request.setStatus(PermissionStatus.REJECTED);
            log.info("权限已拒绝: {}", requestId);
        }
    }

    public PermissionStatus checkStatus(String requestId) {
        PermissionRequest request = pendingRequests.get(requestId);
        return request != null ? request.getStatus() : PermissionStatus.REJECTED;
    }

    public boolean isApproved(String requestId) {
        return checkStatus(requestId) == PermissionStatus.APPROVED;
    }

    public void addToWhitelist(String command) {
        whitelist.put(command, true);
        log.info("命令已加入白名单: {}", command);
    }

    public boolean isWhitelisted(String command) {
        for (String allowed : whitelist.keySet()) {
            if (command.startsWith(allowed) || command.equals(allowed)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getWhitelist() {
        return new ArrayList<>(whitelist.keySet());
    }

    public void removeFromWhitelist(String command) {
        whitelist.remove(command);
        log.info("命令已从白名单移除: {}", command);
    }

    public void removeRequest(String requestId) {
        pendingRequests.remove(requestId);
    }
}
