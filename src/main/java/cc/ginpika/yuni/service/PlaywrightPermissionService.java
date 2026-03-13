package cc.ginpika.yuni.service;

import cc.ginpika.yuni.entity.BrowserWhitelist;
import cc.ginpika.yuni.repository.BrowserWhitelistRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PlaywrightPermissionService {

    @Resource
    private BrowserWhitelistRepository browserWhitelistRepository;

    @Resource
    private SimpMessagingTemplate messagingTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private final Map<String, PermissionRequest> pendingRequests = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public boolean isDomainAllowed(String url) {
        try {
            String domain = extractDomain(url);
            return browserWhitelistRepository.existsByDomain(domain);
        } catch (Exception e) {
            log.error("提取域名失败: {}", url, e);
            return false;
        }
    }

    @Transactional
    public void addToWhitelist(String domain) {
        if (!browserWhitelistRepository.existsByDomain(domain)) {
            BrowserWhitelist whitelist = new BrowserWhitelist();
            whitelist.setDomain(domain);
            browserWhitelistRepository.save(whitelist);
            log.info("添加域名到白名单: {}", domain);
        }
    }

    @Transactional
    public void removeFromWhitelist(String domain) {
        browserWhitelistRepository.deleteByDomain(domain);
        log.info("从白名单移除域名: {}", domain);
    }

    @Transactional(readOnly = true)
    public List<BrowserWhitelist> getWhitelist() {
        return browserWhitelistRepository.findAll();
    }

    public String requestDomainPermission(String url, String reason) {
        String requestId = UUID.randomUUID().toString();
        String domain = extractDomain(url);
        
        PermissionRequest request = new PermissionRequest(requestId, domain, url, reason);
        pendingRequests.put(requestId, request);
        
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "browser_permission_request");
            message.put("requestId", requestId);
            message.put("domain", domain);
            message.put("url", url);
            message.put("reason", reason);
            
            messagingTemplate.convertAndSend("/topic/browser-permission", objectMapper.writeValueAsString(message));
            log.info("发送浏览器权限请求: domain={}, requestId={}", domain, requestId);
        } catch (Exception e) {
            log.error("发送浏览器权限请求失败", e);
        }
        
        return requestId;
    }

    public PermissionRequest getPendingRequest(String requestId) {
        return pendingRequests.get(requestId);
    }

    public void removePendingRequest(String requestId) {
        pendingRequests.remove(requestId);
    }

    public void handlePermissionResponse(String requestId, boolean approved, boolean remember) {
        PermissionRequest request = pendingRequests.get(requestId);
        if (request != null) {
            request.setApproved(approved);
            request.setRemember(remember);
            synchronized (request) {
                request.notifyAll();
            }
            
            if (approved && remember) {
                addToWhitelist(request.getDomain());
            }
            
            log.info("处理浏览器权限响应: domain={}, approved={}, remember={}", 
                    request.getDomain(), approved, remember);
        }
    }

    private String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            if (domain != null && domain.startsWith("www.")) {
                domain = domain.substring(4);
            }
            return domain;
        } catch (Exception e) {
            return url;
        }
    }

    public static class PermissionRequest {
        private final String requestId;
        private final String domain;
        private final String url;
        private final String reason;
        private Boolean approved;
        private Boolean remember;

        public PermissionRequest(String requestId, String domain, String url, String reason) {
            this.requestId = requestId;
            this.domain = domain;
            this.url = url;
            this.reason = reason;
        }

        public String getRequestId() { return requestId; }
        public String getDomain() { return domain; }
        public String getUrl() { return url; }
        public String getReason() { return reason; }
        public Boolean getApproved() { return approved; }
        public void setApproved(Boolean approved) { this.approved = approved; }
        public Boolean getRemember() { return remember; }
        public void setRemember(Boolean remember) { this.remember = remember; }
    }
}
