package cc.ginpika.yuni.repository;

import cc.ginpika.yuni.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, String> {
    Optional<SessionEntity> findBySessionId(String sessionId);
    void deleteBySessionId(String sessionId);
    List<SessionEntity> findTop5ByOrderByUpdatedAtDesc();
    List<SessionEntity> findAllByOrderByUpdatedAtDesc();
}
