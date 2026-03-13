package cc.ginpika.yuni.tool;

import cc.ginpika.yuni.service.ToolConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ToolRegistry {
    private final Map<String, Tool> tools = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Resource
    @Lazy
    private ToolConfigService toolConfigService;

    public void register(Tool tool) {
        tools.put(tool.getName(), tool);
        log.info("注册工具: {}", tool.getName());
    }

    public void unregister(String name) {
        tools.remove(name);
        log.info("注销工具: {}", name);
    }

    public Tool getTool(String name) {
        return tools.get(name);
    }

    public Collection<Tool> getAllTools() {
        return tools.values();
    }

    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }

    public ArrayNode buildToolsSchema() {
        ArrayNode toolsArray = objectMapper.createArrayNode();
        for (Tool tool : tools.values()) {
            if (toolConfigService.isToolEnabled(tool.getName())) {
                ObjectNode toolDef = objectMapper.createObjectNode();
                toolDef.put("type", "function");
                
                ObjectNode functionDef = objectMapper.createObjectNode();
                functionDef.put("name", tool.getName());
                functionDef.put("description", tool.getDescription());
                functionDef.set("parameters", tool.getParametersSchema());
                
                toolDef.set("function", functionDef);
                toolsArray.add(toolDef);
            }
        }
        return toolsArray;
    }

    public String buildToolsSchemaString() {
        return buildToolsSchema().toString();
    }
}
