package cc.ginpika.yuni.controller;

import cc.ginpika.yuni.entity.ToolConfig;
import cc.ginpika.yuni.service.ToolConfigService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tool-config")
public class ToolConfigController {

    @Resource
    private ToolConfigService toolConfigService;

    @GetMapping("/list")
    public Map<String, Object> listToolConfigs() {
        Map<String, Object> result = new HashMap<>();
        List<ToolConfig> configs = toolConfigService.getAllToolConfigs();
        result.put("configs", configs);
        result.put("total", configs.size());
        return result;
    }

    @PostMapping("/toggle/{toolName}")
    public Map<String, Object> toggleTool(@PathVariable String toolName) {
        Map<String, Object> result = new HashMap<>();
        List<ToolConfig> configs = toolConfigService.getAllToolConfigs();
        ToolConfig config = configs.stream()
                .filter(c -> c.getToolName().equals(toolName))
                .findFirst()
                .orElse(null);
        
        if (config != null) {
            boolean newState = !config.getEnabled();
            ToolConfig updated = toolConfigService.setToolEnabled(toolName, newState);
            result.put("success", true);
            result.put("toolName", toolName);
            result.put("enabled", updated.getEnabled());
        } else {
            result.put("success", false);
            result.put("message", "工具不存在");
        }
        return result;
    }

    @PostMapping("/set/{toolName}")
    public Map<String, Object> setToolEnabled(@PathVariable String toolName, @RequestParam boolean enabled) {
        Map<String, Object> result = new HashMap<>();
        ToolConfig config = toolConfigService.setToolEnabled(toolName, enabled);
        if (config != null) {
            result.put("success", true);
            result.put("toolName", toolName);
            result.put("enabled", config.getEnabled());
        } else {
            result.put("success", false);
            result.put("message", "工具不存在");
        }
        return result;
    }

    @PostMapping("/batch")
    public Map<String, Object> batchSetEnabled(@RequestBody Map<String, Boolean> toolStates) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Boolean> states = toolConfigService.setMultipleEnabled(toolStates);
        result.put("success", true);
        result.put("states", states);
        return result;
    }
}
