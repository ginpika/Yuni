package cc.ginpika.yuni.repository;

import cc.ginpika.yuni.entity.BrowserWhitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrowserWhitelistRepository extends JpaRepository<BrowserWhitelist, Long> {
    Optional<BrowserWhitelist> findByDomain(String domain);
    boolean existsByDomain(String domain);
    void deleteByDomain(String domain);
}
