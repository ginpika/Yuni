package cc.ginpika.yuni.controller;

import cc.ginpika.yuni.entity.BrowserWhitelist;
import cc.ginpika.yuni.service.PlaywrightPermissionService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/browser")
public class BrowserPermissionController {

    @Resource
    private PlaywrightPermissionService permissionService;

    @GetMapping("/whitelist")
    public Map<String, Object> getWhitelist() {
        Map<String, Object> result = new HashMap<>();
        List<BrowserWhitelist> whitelist = permissionService.getWhitelist();
        result.put("whitelist", whitelist);
        result.put("total", whitelist.size());
        return result;
    }

    @PostMapping("/whitelist")
    public Map<String, Object> addToWhitelist(@RequestParam String domain) {
        Map<String, Object> result = new HashMap<>();
        permissionService.addToWhitelist(domain);
        result.put("success", true);
        result.put("message", "已添加到白名单");
        return result;
    }

    @DeleteMapping("/whitelist/{domain}")
    public Map<String, Object> removeFromWhitelist(@PathVariable String domain) {
        Map<String, Object> result = new HashMap<>();
        permissionService.removeFromWhitelist(domain);
        result.put("success", true);
        result.put("message", "已从白名单移除");
        return result;
    }

    @PostMapping("/permission/response")
    public Map<String, Object> handlePermissionResponse(
            @RequestParam String requestId,
            @RequestParam boolean approved,
            @RequestParam(defaultValue = "false") boolean remember) {
        Map<String, Object> result = new HashMap<>();
        permissionService.handlePermissionResponse(requestId, approved, remember);
        result.put("success", true);
        return result;
    }
}
