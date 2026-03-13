package cc.ginpika.yuni.service;

import cc.ginpika.yuni.core.YuniMessage;
import cc.ginpika.yuni.core.YuniSession;
import cc.ginpika.yuni.entity.MessageEntity;
import cc.ginpika.yuni.entity.SessionEntity;
import cc.ginpika.yuni.repository.MessageRepository;
import cc.ginpika.yuni.repository.SessionRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class SessionManager {

    @Resource
    private SessionRepository sessionRepository;

    @Resource
    private MessageRepository messageRepository;

    @Transactional
    public YuniSession createSession() {
        String sessionId = UUID.randomUUID().toString();
        
        SessionEntity sessionEntity = new SessionEntity();
        sessionEntity.setSessionId(sessionId);
        sessionEntity.setCreatedAt(LocalDateTime.now());
        sessionEntity.setUpdatedAt(LocalDateTime.now());
        sessionEntity.setStatus("active");
        sessionRepository.save(sessionEntity);
        
        YuniSession session = new YuniSession(sessionId);
        log.info("创建新会话: {}", sessionId);
        return session;
    }

    @Transactional(readOnly = true)
    public YuniSession getSession(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        
        SessionEntity sessionEntity = sessionRepository.findBySessionId(sessionId)
                .orElse(null);
        
        if (sessionEntity == null) {
            return null;
        }
        
        YuniSession session = new YuniSession(sessionId);
        
        List<MessageEntity> messageEntities = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        for (MessageEntity msgEntity : messageEntities) {
            YuniMessage message = YuniMessage.builder()
                    .role(msgEntity.getRole())
                    .content(msgEntity.getContent())
                    .rawResponse(msgEntity.getRawResponse())
                    .build();
            session.getMessages().add(message);
        }
        
        return session;
    }

    @Transactional
    public void saveMessage(String sessionId, YuniMessage message) {
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setSessionId(sessionId);
        messageEntity.setRole(message.getRole());
        messageEntity.setContent(message.getContent());
        messageEntity.setRawResponse(message.getRawResponse());
        messageEntity.setCreatedAt(LocalDateTime.now());
        messageRepository.save(messageEntity);
        
        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            session.setUpdatedAt(LocalDateTime.now());
            sessionRepository.save(session);
        });
        
        log.debug("保存消息: sessionId={}, role={}", sessionId, message.getRole());
    }

    @Transactional
    public void deleteSession(String sessionId) {
        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteBySessionId(sessionId);
        log.info("删除会话: {}", sessionId);
    }

    @Transactional(readOnly = true)
    public List<SessionEntity> getAllSessions() {
        return sessionRepository.findAll();
    }
}
