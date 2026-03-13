package cc.ginpika.yuni.controller;

import cc.ginpika.yuni.entity.MessageEntity;
import cc.ginpika.yuni.entity.SessionEntity;
import cc.ginpika.yuni.repository.MessageRepository;
import cc.ginpika.yuni.repository.SessionRepository;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/database")
public class DatabaseController {

    @Resource
    private SessionRepository sessionRepository;

    @Resource
    private MessageRepository messageRepository;

    @GetMapping("/tables")
    public Map<String, Object> getTables() {
        Map<String, Object> result = new HashMap<>();
        
        List<SessionEntity> sessions = sessionRepository.findAll();
        List<MessageEntity> messages = messageRepository.findAll();
        
        result.put("success", true);
        result.put("tables", List.of(
                Map.of(
                        "name", "sessions",
                        "rowCount", sessions.size(),
                        "description", "会话表"
                ),
                Map.of(
                        "name", "messages",
                        "rowCount", messages.size(),
                        "description", "消息表"
                )
        ));
        
        return result;
    }

    @GetMapping("/sessions")
    public Map<String, Object> getSessions() {
        Map<String, Object> result = new HashMap<>();
        List<SessionEntity> sessions = sessionRepository.findAll();
        result.put("success", true);
        result.put("data", sessions);
        return result;
    }

    @GetMapping("/messages")
    public Map<String, Object> getMessages(
            @RequestParam(required = false) String sessionId) {
        Map<String, Object> result = new HashMap<>();
        List<MessageEntity> messages;
        
        if (sessionId != null && !sessionId.isEmpty()) {
            messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        } else {
            messages = messageRepository.findAll();
        }
        
        result.put("success", true);
        result.put("data", messages);
        return result;
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Map<String, Object> deleteSession(@PathVariable String sessionId) {
        Map<String, Object> result = new HashMap<>();
        
        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteBySessionId(sessionId);
        
        result.put("success", true);
        result.put("message", "会话删除成功");
        return result;
    }

    @DeleteMapping("/messages/{id}")
    public Map<String, Object> deleteMessage(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        
        messageRepository.deleteById(id);
        
        result.put("success", true);
        result.put("message", "消息删除成功");
        return result;
    }
}
