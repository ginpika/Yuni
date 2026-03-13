package cc.ginpika.yuni.service;

import cc.ginpika.yuni.config.YuniConfig;
import cc.ginpika.yuni.core.ChatResponse;
import cc.ginpika.yuni.core.YuniMessage;
import cc.ginpika.yuni.core.YuniReply;
import cc.ginpika.yuni.core.YuniSession;
import cc.ginpika.yuni.entity.SessionEntity;
import cc.ginpika.yuni.tool.ToolExecutor;
import cc.ginpika.yuni.tool.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

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

    public static final String SYSTEM_PROMPT = "你是一个 AI 助手可以帮助用户完成各种任务。你也可以是任何人，遵从用户的指令";

    @PostConstruct
    public void init() {
        List<SessionEntity> sessions = sessionManager.getAllSessions();
        
        if (sessions.isEmpty()) {
            systemSession = sessionManager.createSession();
            log.info("创建新会话: {}", systemSession.getSessionId());
        } else {
            SessionEntity latestSession = sessions.getLast();
            systemSession = sessionManager.getSession(latestSession.getSessionId());
            log.info("加载已有会话: {}, 消息数: {}", systemSession.getSessionId(), systemSession.getMessages().size());
        }
    }

    public ChatResponse call(String userInput) throws IOException {
        log.info("用户输入：{}", userInput);
        return call(systemSession, userInput);
    }

    public ChatResponse call(YuniSession session, String input) throws IOException {
        YuniMessage userMsg = YuniMessage.builder().role("user").content(input).build();
        session.getMessages().add(userMsg);
        sessionManager.saveMessage(session.getSessionId(), userMsg);
        return reasoningAndActing(session);
    }

    public ChatResponse reasoningAndActing(YuniSession session) throws IOException {
        String rawResponse = aiClient.call(buildRequestBody(session));
        log.info("AI 原始响应: {}", rawResponse);

        // "/choices/0/message/content"
        
        try {
            JsonNode fullResponse = objectMapper.readTree(rawResponse);
            JsonNode messageNode = fullResponse.at("/choices/0/message");
            String finishReason = fullResponse.at("/choices/0/finish_reason").asText();
            String content = fullResponse.at("/choices/0/message/content").asText();

            log.info("finishReason: {}", finishReason);
            log.info("content: {}", content);

            switch (finishReason) {
                case "tool_calls" -> {
                    JsonNode toolCall = messageNode.get("tool_calls");
                    toolCall.forEach(node -> {
                        ToolExecutor.ToolResult result = toolExecutor.executeFromJson(node);
                        YuniMessage toolResultMsg = YuniMessage.builder()
                                .role("tool")
                                .content(result.toJson())
                                .rawResponse(rawResponse)
                                .build();
                        session.getMessages().add(toolResultMsg);
                        sessionManager.saveMessage(session.getSessionId(), toolResultMsg);
                    });
                    return reasoningAndActing(session);
                }
                case "stop" -> {
                    YuniMessage assistantMsg = YuniMessage.builder()
                            .role("assistant")
                            .content(content)
                            .rawResponse(rawResponse)
                            .build();
                    session.getMessages().add(assistantMsg);
                    sessionManager.saveMessage(session.getSessionId(), assistantMsg);
                    log.info("因 stop 退出");
                    return ChatResponse.builder()
                            .message(content)
                            .rawResponse(rawResponse)
                            .success(true)
                            .build();
                }
                case "length" -> {
                    YuniMessage assistantMsg = YuniMessage.builder()
                            .role("assistant")
                            .content(content)
                            .rawResponse(rawResponse)
                            .build();
                    session.getMessages().add(assistantMsg);
                    sessionManager.saveMessage(session.getSessionId(), assistantMsg);
                    log.info("因 length 退出");
                    return ChatResponse.builder()
                            .message(content)
                            .rawResponse(rawResponse)
                            .success(true)
                            .build();
                }
            }
            return ChatResponse.builder()
                    .message("Error")
                    .rawResponse(rawResponse)
                    .success(false)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
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
    
    public String getCurrentSessionId() {
        return systemSession != null ? systemSession.getSessionId() : null;
    }
    
    public List<YuniMessage> getCurrentSessionMessages() {
        return systemSession != null ? systemSession.getMessages() : List.of();
    }
    
    public int getMessageCount() {
        return systemSession != null ? systemSession.getMessages().size() : 0;
    }
}
