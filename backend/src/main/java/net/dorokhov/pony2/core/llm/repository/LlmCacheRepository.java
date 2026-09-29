package net.dorokhov.pony2.core.llm.repository;

import net.dorokhov.pony2.api.llm.LlmCache;
import net.dorokhov.pony2.api.llm.LlmCacheRegion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface LlmCacheRepository extends JpaRepository<LlmCache, String> {
    Optional<LlmCache> findByRegionAndKeyAndVersion(LlmCacheRegion region, String key, int version);
    long deleteByExpirationDateLessThanEqual(LocalDateTime expirationInSeconds);
}
