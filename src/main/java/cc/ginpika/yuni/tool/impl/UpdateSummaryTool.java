package cc.ginpika.yuni.tool.impl;

import cc.ginpika.yuni.repository.SessionRepository;
import cc.ginpika.yuni.service.AgentContext;
import cc.ginpika.yuni.tool.AbstractTool;
import cc.ginpika.yuni.tool.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UpdateSummaryTool extends AbstractTool {

    @Resource
    private ToolRegistry toolRegistry;

    @Resource
    private AgentContext agentContext;

    @Resource
    private SessionRepository sessionRepository;

    public UpdateSummaryTool() {
        super("update_summary", "更新当前会话的摘要，用于描述会话的主要内容或主题");
    }

    @PostConstruct
    public void register() {
        toolRegistry.register(this);
    }

    @Override
    protected ObjectNode buildProperties() {
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("summary", createProperty("string", "会话摘要，简短描述会话的主要内容，不超过100字"));
        return properties;
    }

    @Override
    protected String[] getRequiredParameters() {
        return new String[]{"summary"};
    }

    @Override
    public String execute(JsonNode arguments) {
        String summary = arguments.has("summary") ? arguments.get("summary").asText() : null;
        
        if (summary == null || summary.trim().isEmpty()) {
            return "{\"success\": false, \"error\": \"摘要不能为空\"}";
        }

        String sessionId = agentContext.getCurrentSessionId();
        if (sessionId == null) {
            return "{\"success\": false, \"error\": \"当前没有活动会话\"}";
        }

        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            String trimmedSummary = summary.length() > 100 ? summary.substring(0, 100) : summary;
            session.setSummary(trimmedSummary);
            sessionRepository.save(session);
            log.info("更新会话摘要: sessionId={}, summary={}", sessionId, trimmedSummary);
        });

        return "{\"success\": true, \"result\": \"摘要已更新\"}";
    }
}
