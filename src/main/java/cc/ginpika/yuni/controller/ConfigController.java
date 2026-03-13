package cc.ginpika.yuni.controller;

import cc.ginpika.yuni.config.YuniConfig;
import cc.ginpika.yuni.core.YuniMessage;
import cc.ginpika.yuni.service.AgentContext;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ConfigController {

    @Resource
    private YuniConfig yuniConfig;

    @Resource
    private AgentContext agentContext;

    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("contextWindow", yuniConfig.getContextWindow());
        config.put("maxTokens", yuniConfig.getMaxTokens());
        config.put("model", yuniConfig.getModel());
        return config;
    }

    @GetMapping("/usage")
    public Map<String, Object> getUsage() {
        Map<String, Object> usage = new HashMap<>();
        
        List<YuniMessage> messages = agentContext.getCurrentSessionMessages();
        int currentTokens = estimateTokens(messages);
        int contextWindow = yuniConfig.getContextWindow();
        double percentage = contextWindow > 0 ? (double) currentTokens / contextWindow * 100 : 0;
        
        usage.put("currentTokens", currentTokens);
        usage.put("contextWindow", contextWindow);
        usage.put("percentage", Math.min(percentage, 100));
        usage.put("messageCount", messages.size());
        
        return usage;
    }

    private int estimateTokens(List<YuniMessage> messages) {
        return messages.stream()
                .mapToInt(msg -> estimateTokensForText(msg.getContent()))
                .sum();
    }

    private int estimateTokensForText(String text) {
        if (text == null) return 0;
        int chineseChars = 0;
        int otherChars = 0;
        
        for (char c : text.toCharArray()) {
            if (Character.toString(c).matches("[\\u4e00-\\u9fa5]")) {
                chineseChars++;
            } else {
                otherChars++;
            }
        }
        
        return (int) (chineseChars * 1.5 + otherChars / 4.0);
    }
}
