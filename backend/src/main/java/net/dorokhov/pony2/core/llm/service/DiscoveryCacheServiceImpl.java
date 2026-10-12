package net.dorokhov.pony2.core.llm.service;

import net.dorokhov.pony2.api.llm.domain.DiscoveryCache;
import net.dorokhov.pony2.api.llm.domain.DiscoveryCacheRegion;
import net.dorokhov.pony2.api.llm.service.DiscoveryCacheService;
import net.dorokhov.pony2.core.llm.repository.DiscoveryCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class DiscoveryCacheServiceImpl implements DiscoveryCacheService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DiscoveryCacheRepository discoveryCacheRepository;

    public DiscoveryCacheServiceImpl(DiscoveryCacheRepository discoveryCacheRepository) {
        this.discoveryCacheRepository = discoveryCacheRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> get(DiscoveryCacheRegion region, String key, int version) {
        LocalDateTime now = LocalDateTime.now();
        return discoveryCacheRepository.findByRegionAndKeyAndVersion(region, key, version)
                .filter(cache -> cache.getExpirationDate().isAfter(now))
                .map(DiscoveryCache::getValue);
    }

    @Override
    @Transactional
    public DiscoveryCache put(DiscoveryCacheRegion region, String key, int version, String value) {
        LocalDateTime now = LocalDateTime.now();
        DiscoveryCache cache = discoveryCacheRepository.findByRegionAndKeyAndVersion(region, key, version)
                .orElseGet(DiscoveryCache::new);
        return discoveryCacheRepository.save(cache
                .setRegion(region)
                .setKey(key)
                .setVersion(version)
                .setValue(value)
                .setCreationDate(now)
                .setExpirationDate(now.plusSeconds(region.getExpirationInSeconds())));
    }

    @Override
    @Transactional
    public String getOrPut(DiscoveryCacheRegion region, String key, int version, Supplier<String> valueSupplier) {
        return get(region, key, version)
                .orElseGet(() -> put(region, key, version, valueSupplier.get()).getValue());
    }

    @Override
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public long deleteExpired() {
        long deletedCount = discoveryCacheRepository.deleteByExpirationDateLessThanEqual(LocalDateTime.now());
        logger.info("Deleted {} expired discovery cache entries.", deletedCount);
        return deletedCount;
    }

    @Override
    @Transactional
    public long clear() {
        long count = discoveryCacheRepository.count();
        discoveryCacheRepository.deleteAllInBatch();
        return count;
    }

    @Override
    @Transactional
    public long clear(DiscoveryCacheRegion region) {
        return discoveryCacheRepository.deleteByRegion(region);
    }

}
