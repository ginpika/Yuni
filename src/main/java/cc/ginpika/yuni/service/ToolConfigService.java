package cc.ginpika.yuni.service;

import cc.ginpika.yuni.entity.ToolConfig;
import cc.ginpika.yuni.repository.ToolConfigRepository;
import cc.ginpika.yuni.tool.Tool;
import cc.ginpika.yuni.tool.ToolRegistry;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class ToolConfigService {

    @Resource
    private ToolConfigRepository toolConfigRepository;

    @Resource
    private ToolRegistry toolRegistry;

    public void syncToolConfigs() {
        Collection<Tool> tools = toolRegistry.getAllTools();
        log.info("同步工具配置，当前工具数量: {}", tools.size());
        for (Tool tool : tools) {
            Optional<ToolConfig> existing = toolConfigRepository.findByToolName(tool.getName());
            if (existing.isEmpty()) {
                ToolConfig config = new ToolConfig();
                config.setToolName(tool.getName());
                config.setDescription(tool.getDescription());
                config.setEnabled(true);
                toolConfigRepository.save(config);
                log.info("初始化工具配置: {}", tool.getName());
            } else {
                ToolConfig config = existing.get();
                config.setDescription(tool.getDescription());
                toolConfigRepository.save(config);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ToolConfig> getAllToolConfigs() {
        syncToolConfigs();
        return toolConfigRepository.findAll();
    }

    @Transactional(readOnly = true)
    public boolean isToolEnabled(String toolName) {
        return toolConfigRepository.findByToolName(toolName)
                .map(ToolConfig::getEnabled)
                .orElse(true);
    }

    @Transactional
    public ToolConfig setToolEnabled(String toolName, boolean enabled) {
        Optional<ToolConfig> existing = toolConfigRepository.findByToolName(toolName);
        if (existing.isPresent()) {
            ToolConfig config = existing.get();
            config.setEnabled(enabled);
            return toolConfigRepository.save(config);
        }
        return null;
    }

    @Transactional
    public Map<String, Boolean> setMultipleEnabled(Map<String, Boolean> toolStates) {
        Map<String, Boolean> result = new HashMap<>();
        for (Map.Entry<String, Boolean> entry : toolStates.entrySet()) {
            ToolConfig config = setToolEnabled(entry.getKey(), entry.getValue());
            result.put(entry.getKey(), config != null && config.getEnabled());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Set<String> getEnabledToolNames() {
        Set<String> enabledTools = new HashSet<>();
        toolConfigRepository.findAllByEnabledTrue().forEach(config -> 
            enabledTools.add(config.getToolName())
        );
        return enabledTools;
    }
}
