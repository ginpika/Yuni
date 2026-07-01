package cc.ginpika.yuni.repository;

import cc.ginpika.yuni.entity.ToolConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ToolConfigRepository extends JpaRepository<ToolConfig, Long> {
    Optional<ToolConfig> findByToolName(String toolName);
    List<ToolConfig> findAllByEnabledTrue();
    void deleteByToolName(String toolName);
}
