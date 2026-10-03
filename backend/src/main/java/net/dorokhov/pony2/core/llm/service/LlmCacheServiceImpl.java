package net.dorokhov.pony2.core.llm.service;

import net.dorokhov.pony2.api.llm.domain.LlmCache;
import net.dorokhov.pony2.api.llm.domain.LlmCacheRegion;
import net.dorokhov.pony2.api.llm.service.LlmCacheService;
import net.dorokhov.pony2.core.llm.repository.LlmCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class LlmCacheServiceImpl implements LlmCacheService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LlmCacheRepository llmCacheRepository;

    public LlmCacheServiceImpl(LlmCacheRepository llmCacheRepository) {
        this.llmCacheRepository = llmCacheRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> get(LlmCacheRegion region, String key, int version) {
        LocalDateTime now = LocalDateTime.now();
        return llmCacheRepository.findByRegionAndKeyAndVersion(region, key, version)
                .filter(cache -> cache.getExpirationDate().isAfter(now))
                .map(LlmCache::getValue);
    }

    @Override
    @Transactional
    public LlmCache put(LlmCacheRegion region, String key, int version, String value) {
        LocalDateTime now = LocalDateTime.now();
        LlmCache cache = llmCacheRepository.findByRegionAndKeyAndVersion(region, key, version)
                .orElseGet(LlmCache::new);
        return llmCacheRepository.save(cache
                .setRegion(region)
                .setKey(key)
                .setVersion(version)
                .setValue(value)
                .setCreationDate(now)
                .setExpirationDate(now.plusSeconds(region.getExpirationInSeconds())));
    }

    @Override
    @Transactional
    public String getOrPut(LlmCacheRegion region, String key, int version, Supplier<String> valueSupplier) {
        return get(region, key, version)
                .orElseGet(() -> put(region, key, version, valueSupplier.get()).getValue());
    }

    @Override
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public long deleteExpired() {
        long deletedCount = llmCacheRepository.deleteByExpirationDateLessThanEqual(LocalDateTime.now());
        logger.info("Deleted {} expired LLM cache entries.", deletedCount);
        return deletedCount;
    }

    @Override
    @Transactional
    public long clear() {
        long count = llmCacheRepository.count();
        llmCacheRepository.deleteAllInBatch();
        return count;
    }

    @Override
    @Transactional
    public long clear(LlmCacheRegion region) {
        return llmCacheRepository.deleteByRegion(region);
    }

}
