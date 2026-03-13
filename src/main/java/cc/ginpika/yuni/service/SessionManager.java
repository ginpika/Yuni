package cc.ginpika.yuni.service;

import cc.ginpika.yuni.core.YuniSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SessionManager {
    Map<String, YuniSession> sessions = new ConcurrentHashMap<>();

    public YuniSession createSession() {
        return new YuniSession();
    }

    public YuniSession getSession(String uuid) {
        return sessions.get(uuid);
    }

    public void deleteSession(String uuid) {
        sessions.remove(uuid);
    }
}
