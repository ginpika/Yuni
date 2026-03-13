package cc.ginpika.yuni.tool.impl;

import cc.ginpika.yuni.tool.AbstractTool;
import cc.ginpika.yuni.tool.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class TimeTool extends AbstractTool {

    @Resource
    private ToolRegistry toolRegistry;

    public TimeTool() {
        super("get_current_time", "获取当前时间，可以指定时区");
    }

    @PostConstruct
    public void register() {
        toolRegistry.register(this);
    }

    @Override
    protected ObjectNode buildProperties() {
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("timezone", createProperty("string", "时区，如：Asia/Shanghai, America/New_York"));
        properties.set("format", createProperty("string", "时间格式：datetime(日期时间), date(仅日期), time(仅时间)"));
        return properties;
    }

    @Override
    protected String[] getRequiredParameters() {
        return new String[]{};
    }

    @Override
    public String execute(JsonNode arguments) {
        String timezone = arguments.has("timezone") ? arguments.get("timezone").asText() : "Asia/Shanghai";
        String format = arguments.has("format") ? arguments.get("format").asText() : "datetime";
        
        try {
            ZoneId zoneId = ZoneId.of(timezone);
            LocalDateTime now = LocalDateTime.now(zoneId);
            
            String result;
            switch (format) {
                case "date":
                    result = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    break;
                case "time":
                    result = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    break;
                default:
                    result = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            
            return String.format("{\"timezone\": \"%s\", \"time\": \"%s\"}", timezone, result);
        } catch (Exception e) {
            return String.format("{\"error\": \"获取时间失败: %s\"}", e.getMessage());
        }
    }
}
