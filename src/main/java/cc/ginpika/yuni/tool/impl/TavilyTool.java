package cc.ginpika.yuni.tool.impl;

import cc.ginpika.yuni.config.YuniConfig;
import cc.ginpika.yuni.tool.AbstractTool;
import cc.ginpika.yuni.tool.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class TavilyTool extends AbstractTool {

    @Resource
    private ToolRegistry toolRegistry;

    @Resource
    private YuniConfig yuniConfig;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String TAVILY_API_URL = "https://api.tavily.com/search";

    public TavilyTool() {
        super("tavily_search", "使用 Tavily 搜索引擎搜索互联网信息，返回相关搜索结果。当需要查找最新信息、新闻、技术文档或任何需要联网查询的内容时使用此工具。");
    }

    @PostConstruct
    public void register() {
        toolRegistry.register(this);
    }

    @Override
    protected ObjectNode buildProperties() {
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("query", createProperty("string", "搜索查询关键词或问题"));
        properties.set("search_depth", createProperty("string", "搜索深度：basic（快速搜索）或 advanced（深度搜索，默认为 basic）", new String[]{"basic", "advanced"}));
        properties.set("max_results", createProperty("integer", "返回结果的最大数量，默认为 5"));
        return properties;
    }

    @Override
    protected String[] getRequiredParameters() {
        return new String[]{"query"};
    }

    @Override
    public String execute(JsonNode arguments) {
        String query = arguments.has("query") ? arguments.get("query").asText() : null;
        
        if (query == null || query.trim().isEmpty()) {
            return "{\"success\": false, \"error\": \"搜索查询不能为空\"}";
        }

        String apiKey = yuniConfig.getTavilyApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return "{\"success\": false, \"error\": \"Tavily API Key 未配置，请在配置文件中设置 yuni.tavily-api-key 或环境变量 TAVILY_API_KEY\"}";
        }

        String searchDepth = arguments.has("search_depth") ? arguments.get("search_depth").asText() : "basic";
        int maxResults = arguments.has("max_results") ? arguments.get("max_results").asInt() : 5;
        maxResults = Math.min(Math.max(maxResults, 1), 10);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("api_key", apiKey);
            requestBody.put("query", query);
            requestBody.put("search_depth", searchDepth);
            requestBody.put("max_results", maxResults);
            requestBody.put("include_answer", true);
            requestBody.put("include_raw_content", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("调用 Tavily 搜索: query={}, depth={}, maxResults={}", query, searchDepth, maxResults);
            
            JsonNode response = restTemplate.postForObject(TAVILY_API_URL, entity, JsonNode.class);
            
            if (response != null && response.has("results")) {
                StringBuilder result = new StringBuilder();
                result.append("{\"success\": true, \"query\": \"").append(escapeJson(query)).append("\"");
                
                if (response.has("answer") && !response.get("answer").isNull()) {
                    result.append(", \"answer\": \"").append(escapeJson(response.get("answer").asText())).append("\"");
                }
                
                result.append(", \"results\": [");
                
                JsonNode results = response.get("results");
                boolean first = true;
                for (JsonNode item : results) {
                    if (!first) result.append(", ");
                    first = false;
                    
                    result.append("{");
                    result.append("\"title\": \"").append(escapeJson(item.has("title") ? item.get("title").asText() : "")).append("\", ");
                    result.append("\"url\": \"").append(escapeJson(item.has("url") ? item.get("url").asText() : "")).append("\", ");
                    result.append("\"content\": \"").append(escapeJson(item.has("content") ? item.get("content").asText() : "")).append("\"");
                    result.append("}");
                }
                
                result.append("]}");
                return result.toString();
            } else {
                return "{\"success\": false, \"error\": \"搜索返回空结果\"}";
            }
        } catch (Exception e) {
            log.error("Tavily 搜索失败", e);
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
