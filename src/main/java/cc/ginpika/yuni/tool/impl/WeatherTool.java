package cc.ginpika.yuni.tool.impl;

import cc.ginpika.yuni.tool.AbstractTool;
import cc.ginpika.yuni.tool.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class WeatherTool extends AbstractTool {

    @Resource
    private ToolRegistry toolRegistry;

    public WeatherTool() {
        super("get_weather", "获取指定城市的天气信息");
    }

    @PostConstruct
    public void register() {
        toolRegistry.register(this);
    }

    @Override
    protected ObjectNode buildProperties() {
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("city", createProperty("string", "城市名称，如：北京、上海、深圳"));
        properties.set("unit", createProperty("string", "温度单位", new String[]{"celsius", "fahrenheit"}));
        return properties;
    }

    @Override
    protected String[] getRequiredParameters() {
        return new String[]{"city"};
    }

    @Override
    public String execute(JsonNode arguments) {
        String city = arguments.has("city") ? arguments.get("city").asText() : "未知城市";
        String unit = arguments.has("unit") ? arguments.get("unit").asText() : "celsius";
        
        String weather = switch (city) {
            case "北京" -> "晴天，空气质量良好";
            case "上海" -> "多云，有轻微雾霾";
            case "深圳" -> "阴天，可能有小雨";
            case "广州" -> "雷阵雨";
            default -> "晴朗";
        };
        
        int temp = (int) (Math.random() * 20 + 10);
        String tempUnit = "fahrenheit".equals(unit) ? "°F" : "°C";
        if ("fahrenheit".equals(unit)) {
            temp = temp * 9 / 5 + 32;
        }
        
        return String.format("{\"city\": \"%s\", \"weather\": \"%s\", \"temperature\": %d%s}", 
                city, weather, temp, tempUnit);
    }
}
