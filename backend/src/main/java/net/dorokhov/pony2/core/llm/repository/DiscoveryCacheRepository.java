package net.dorokhov.pony2.core.llm.repository;

import net.dorokhov.pony2.api.llm.domain.DiscoveryCache;
import net.dorokhov.pony2.api.llm.domain.DiscoveryCacheRegion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface DiscoveryCacheRepository extends JpaRepository<DiscoveryCache, String> {
    Optional<DiscoveryCache> findByRegionAndKeyAndVersion(DiscoveryCacheRegion region, String key, int version);
    long deleteByExpirationDateLessThanEqual(LocalDateTime expirationDate);
    long deleteByRegion(DiscoveryCacheRegion region);
}
