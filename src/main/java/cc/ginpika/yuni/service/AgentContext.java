package cc.ginpika.yuni.service;

import cc.ginpika.yuni.config.YuniConfig;
import cc.ginpika.yuni.core.ChatResponse;
import cc.ginpika.yuni.core.YuniMessage;
import cc.ginpika.yuni.core.YuniReply;
import cc.ginpika.yuni.core.YuniSession;
import cc.ginpika.yuni.tool.ToolExecutor;
import cc.ginpika.yuni.tool.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class AgentContext {
    @Resource
    YuniConfig apiConfig;
    @Resource
    SessionManager sessionManager;
    @Resource
    OpenAIClient aiClient;
    @Resource
    ToolRegistry toolRegistry;
    @Resource
    ToolExecutor toolExecutor;

    ObjectMapper objectMapper = new ObjectMapper();

    YuniSession systemSession;

    public static final String SYSTEM_PROMPT = "你是一个 AI 助手可以帮助用户完成各种任务。你也可以是任何人，遵从用户的指令\n" +
            "回复格式要求：\n" +
            "1. 当你需要调用工具时，输出 JSON 格式: { \"tool_call\": { \"name\": \"工具名称\", \"arguments\": { 参数对象 } } }\n" +
            "2. 当你认为任务完成可以直接回复时，输出 JSON 格式: { \"reply\": true, \"message\": \"你的回答\" }\n" +
            "3. 当你需要更多信息时，输出 JSON 格式: { \"reply\": false, \"message\": \"你的问题或说明\" }\n" +
            "注意：所有回复必须是有效的 JSON 格式。";

    @PostConstruct
    public void init() {
        systemSession = sessionManager.createSession();
    }

    public ChatResponse call(String userInput) throws IOException {
        log.info("用户输入：{}", userInput);
        return call(systemSession, userInput);
    }

    public ChatResponse call(YuniSession session, String input) throws IOException {
        YuniMessage userMsg = YuniMessage.builder().role("user").content(input).build();
        session.getMessages().add(userMsg);
        return reasoningAndActing(session);
    }

    public ChatResponse reasoningAndActing(YuniSession session) throws IOException {
        String rawResponse = aiClient.call(session, buildRequestBody(session));
        log.info("AI 原始响应: {}", rawResponse);
        
        try {
            JsonNode fullResponse = objectMapper.readTree(rawResponse);
            String content = fullResponse.at("/choices/0/message/content").asText();
            
            JsonNode contentNode;
            try {
                contentNode = objectMapper.readTree(content);
            } catch (Exception e) {
                return ChatResponse.builder()
                        .message(content)
                        .rawResponse(rawResponse)
                        .success(true)
                        .build();
            }
            
            if (contentNode.has("tool_call")) {
                JsonNode toolCall = contentNode.get("tool_call");
                ToolExecutor.ToolResult result = toolExecutor.executeFromJson(toolCall);
                
                YuniMessage toolResultMsg = YuniMessage.builder()
                        .role("tool")
                        .content(result.toJson())
                        .build();
                session.getMessages().add(toolResultMsg);
                
                return reasoningAndActing(session);
            }
            
            String message = contentNode.has("message") ? contentNode.get("message").asText() : content;
            
            return ChatResponse.builder()
                    .message(message)
                    .rawResponse(rawResponse)
                    .success(true)
                    .build();
            
        } catch (Exception e) {
            log.warn("解析响应失败: {}", e.getMessage());
            return ChatResponse.builder()
                    .message(rawResponse)
                    .rawResponse(rawResponse)
                    .success(true)
                    .build();
        }
    }

    public String buildRequestBody(YuniSession session) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", "glm-5");
        ArrayNode messages = objectMapper.createArrayNode();
        
        injectSystemPrompt(messages);
        
        session.getMessages().forEach(msg -> {
            ObjectNode raw = objectMapper.createObjectNode();
            raw.put("role", msg.getRole());
            raw.put("content", msg.getContent());
            messages.add(raw);
        });

        injectToolFunctions(request);
        
        request.set("messages", messages);
        return request.toString();
    }

    public static String buildRequestBody(YuniSession session, ObjectMapper objectMapper) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", "glm-5");
        ArrayNode messages = objectMapper.createArrayNode();
        injectSystemPrompt(messages, objectMapper);
        session.getMessages().forEach(msg -> {
            ObjectNode raw = objectMapper.createObjectNode();
            raw.put("role", msg.getRole());
            raw.put("content", msg.getContent());
            messages.add(raw);
        });
        request.set("messages", messages);
        return request.toString();
    }

    public void injectSystemPrompt(ArrayNode messages) {
        ObjectNode raw = objectMapper.createObjectNode();
        raw.put("role", "system");
        raw.put("content", SYSTEM_PROMPT);
        messages.add(raw);
    }

    public static void injectSystemPrompt(ArrayNode message, ObjectMapper objectMapper) {
        ObjectNode raw = objectMapper.createObjectNode();
        raw.put("role", "system");
        raw.put("content", SYSTEM_PROMPT);
        message.add(raw);
    }

    public void injectToolFunctions(ObjectNode request) {
        if (!toolRegistry.getAllTools().isEmpty()) {
            request.set("tools", toolRegistry.buildToolsSchema());
        }
    }

    public void reply(YuniSession session, YuniReply msg) {
        session.reply(msg);
    }
}
