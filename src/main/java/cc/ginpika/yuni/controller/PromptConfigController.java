package cc.ginpika.yuni.controller;

import cc.ginpika.yuni.entity.PromptConfig;
import cc.ginpika.yuni.service.PromptConfigService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/prompt-config")
public class PromptConfigController {
    
    @Resource
    PromptConfigService promptConfigService;
    
    @GetMapping
    public Map<String, Object> getConfig() {
        Map<String, Object> response = new HashMap<>();
        PromptConfig config = promptConfigService.getConfig();
        
        if (config != null) {
            response.put("id", config.getId());
            response.put("systemPrompt", config.getSystemPrompt());
            response.put("personalityPrompt", config.getPersonalityPrompt());
            response.put("enabled", config.getEnabled());
        } else {
            response.put("id", null);
            response.put("systemPrompt", "");
            response.put("personalityPrompt", "");
            response.put("enabled", false);
        }
        
        return response;
    }
    
    @PostMapping
    public Map<String, Object> saveConfig(@RequestBody PromptConfig config) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            PromptConfig saved = promptConfigService.saveConfig(config);
            response.put("success", true);
            response.put("id", saved.getId());
            response.put("systemPrompt", saved.getSystemPrompt());
            response.put("personalityPrompt", saved.getPersonalityPrompt());
            response.put("enabled", saved.getEnabled());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    @GetMapping("/default")
    public Map<String, Object> getDefaultPrompts() {
        return promptConfigService.getDefaultPrompts();
    }
}