package cc.ginpika.yuni.service;

import cc.ginpika.yuni.entity.PromptConfig;
import cc.ginpika.yuni.repository.PromptConfigRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PromptConfigService {
    
    @Resource
    PromptConfigRepository promptConfigRepository;
    
    public PromptConfig getConfig() {
        List<PromptConfig> configs = promptConfigRepository.findAll();
        if (configs.isEmpty()) {
            return null;
        }
        return configs.get(0);
    }
    
    public PromptConfig saveConfig(PromptConfig config) {
        List<PromptConfig> existingConfigs = promptConfigRepository.findAll();
        
        if (existingConfigs.isEmpty()) {
            return promptConfigRepository.save(config);
        } else {
            PromptConfig existing = existingConfigs.get(0);
            existing.setSystemPrompt(config.getSystemPrompt());
            existing.setPersonalityPrompt(config.getPersonalityPrompt());
            existing.setEnabled(config.getEnabled());
            return promptConfigRepository.save(existing);
        }
    }
    
    public Map<String, Object> getDefaultPrompts() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("systemPrompt", "你是一个 AI 助手可以帮助用户完成各种任务。你也可以是任何人，遵从用户的指令");
        defaults.put("personalityPrompt", "");
        return defaults;
    }
}