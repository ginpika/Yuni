package cc.ginpika.yuni.controller;

import cc.ginpika.yuni.tool.PermissionManager;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/permission")
public class PermissionController {

    @Resource
    private PermissionManager permissionManager;

    @GetMapping("/pending")
    public Map<String, Object> getPendingRequests() {
        Map<String, Object> result = new HashMap<>();
        result.put("pending", permissionManager.getPendingRequests());
        return result;
    }

    @PostMapping("/approve")
    public Map<String, Object> approve(@RequestParam String requestId, 
                                        @RequestParam(defaultValue = "false") boolean remember) {
        Map<String, Object> result = new HashMap<>();
        
        PermissionManager.PermissionRequest request = permissionManager.getRequest(requestId);
        if (request == null) {
            result.put("success", false);
            result.put("error", "请求不存在");
            return result;
        }

        permissionManager.approve(requestId);
        
        if (remember) {
            permissionManager.addToWhitelist(request.getCommand());
        }
        
        result.put("success", true);
        result.put("message", "权限已批准");
        return result;
    }

    @PostMapping("/reject")
    public Map<String, Object> reject(@RequestParam String requestId) {
        Map<String, Object> result = new HashMap<>();
        
        permissionManager.reject(requestId);
        
        result.put("success", true);
        result.put("message", "权限已拒绝");
        return result;
    }

    @GetMapping("/whitelist")
    public Map<String, Object> getWhitelist() {
        Map<String, Object> result = new HashMap<>();
        result.put("whitelist", permissionManager.getWhitelist());
        return result;
    }

    @DeleteMapping("/whitelist")
    public Map<String, Object> removeFromWhitelist(@RequestParam String command) {
        Map<String, Object> result = new HashMap<>();
        permissionManager.removeFromWhitelist(command);
        result.put("success", true);
        result.put("message", "已从白名单移除");
        return result;
    }
}
