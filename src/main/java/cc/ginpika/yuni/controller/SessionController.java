package cc.ginpika.yuni.controller;

import cc.ginpika.yuni.core.YuniMessage;
import cc.ginpika.yuni.core.YuniSession;
import cc.ginpika.yuni.entity.MessageEntity;
import cc.ginpika.yuni.entity.SessionEntity;
import cc.ginpika.yuni.repository.MessageRepository;
import cc.ginpika.yuni.service.SessionManager;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/session")
public class SessionController {

    @Resource
    private SessionManager sessionManager;

    @Resource
    private MessageRepository messageRepository;

    @GetMapping("/list")
    public Map<String, Object> listSessions() {
        Map<String, Object> result = new HashMap<>();
        List<SessionEntity> sessions = sessionManager.getAllSessions();
        result.put("sessions", sessions);
        result.put("total", sessions.size());
        return result;
    }

    @GetMapping("/latest")
    public Map<String, Object> getLatestSession() {
        Map<String, Object> result = new HashMap<>();
        List<SessionEntity> sessions = sessionManager.getAllSessions();
        
        if (sessions.isEmpty()) {
            YuniSession newSession = sessionManager.createSession();
            result.put("sessionId", newSession.getSessionId());
            result.put("messages", newSession.getMessages());
        } else {
            SessionEntity latestSession = sessions.get(sessions.size() - 1);
            YuniSession session = sessionManager.getSession(latestSession.getSessionId());
            result.put("sessionId", latestSession.getSessionId());
            result.put("messages", session.getMessages());
        }
        
        return result;
    }

    @GetMapping("/{sessionId}/messages")
    public Map<String, Object> getSessionMessages(@PathVariable String sessionId) {
        Map<String, Object> result = new HashMap<>();
        List<MessageEntity> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        result.put("sessionId", sessionId);
        result.put("messages", messages);
        return result;
    }

    @PostMapping("/create")
    public Map<String, Object> createSession() {
        Map<String, Object> result = new HashMap<>();
        YuniSession session = sessionManager.createSession();
        result.put("success", true);
        result.put("sessionId", session.getSessionId());
        result.put("message", "会话创建成功");
        return result;
    }

    @DeleteMapping("/{sessionId}")
    public Map<String, Object> deleteSession(@PathVariable String sessionId) {
        Map<String, Object> result = new HashMap<>();
        sessionManager.deleteSession(sessionId);
        result.put("success", true);
        result.put("message", "会话删除成功");
        return result;
    }
}
