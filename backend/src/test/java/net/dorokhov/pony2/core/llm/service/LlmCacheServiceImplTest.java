package net.dorokhov.pony2.core.llm.service;

import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.llm.LlmCache;
import net.dorokhov.pony2.api.llm.service.LlmCacheService;
import net.dorokhov.pony2.core.llm.repository.LlmCacheRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static net.dorokhov.pony2.api.llm.LlmCacheRegion.SPOTIFY;
import static org.assertj.core.api.Assertions.assertThat;

public class LlmCacheServiceImplTest extends IntegrationTest {

    @Autowired
    private LlmCacheService llmCacheService;

    @Autowired
    private LlmCacheRepository llmCacheRepository;

    @Test
    public void shouldCacheValue() {

        LlmCache cache = llmCacheService.put(SPOTIFY, "someKey", 1, "someValue");

        assertThat(cache.getId()).isNotNull();
        assertThat(cache.getRegion()).isSameAs(SPOTIFY);
        assertThat(cache.getKey()).isEqualTo("someKey");
        assertThat(cache.getVersion()).isEqualTo(1);
        assertThat(cache.getValue()).isEqualTo("someValue");
        assertThat(cache.getCreationDate()).isNotNull();
        assertThat(cache.getExpirationDate()).isAfter(cache.getCreationDate());
        assertThat(llmCacheService.get(SPOTIFY, "someKey", 1)).contains("someValue");
    }

    @Test
    public void shouldUpdateCachedValue() {

        llmCacheService.put(SPOTIFY, "someKey", 1, "someValue");
        llmCacheService.put(SPOTIFY, "someKey", 1, "someNewValue");

        assertThat(llmCacheRepository.count()).isEqualTo(1);
        assertThat(llmCacheService.get(SPOTIFY, "someKey", 1)).contains("someNewValue");
    }

    @Test
    public void shouldNotReturnExpiredValue() {

        LlmCache cache = llmCacheService.put(SPOTIFY, "someKey", 1, "someValue");
        llmCacheRepository.saveAndFlush(cache.setExpirationDate(LocalDateTime.now().minusSeconds(1)));

        assertThat(llmCacheService.get(SPOTIFY, "someKey", 1)).isEmpty();
    }

    @Test
    public void shouldGetOrPutValue() {

        AtomicInteger supplierCalls = new AtomicInteger();

        String firstValue = llmCacheService.getOrPut(SPOTIFY, "someKey", 1, () -> {
            supplierCalls.incrementAndGet();
            return "someValue";
        });
        String secondValue = llmCacheService.getOrPut(SPOTIFY, "someKey", 1, () -> {
            supplierCalls.incrementAndGet();
            return "someNewValue";
        });

        assertThat(firstValue).isEqualTo("someValue");
        assertThat(secondValue).isEqualTo("someValue");
        assertThat(supplierCalls).hasValue(1);
    }

    @Test
    public void shouldDeleteExpiredValues() {

        LlmCache expiredCache = llmCacheService.put(SPOTIFY, "expiredKey", 1, "expiredValue");
        llmCacheRepository.saveAndFlush(expiredCache.setExpirationDate(LocalDateTime.now().minusSeconds(1)));
        llmCacheService.put(SPOTIFY, "activeKey", 1, "activeValue");

        assertThat(llmCacheService.deleteExpired()).isEqualTo(1);

        assertThat(llmCacheRepository.findByRegionAndKeyAndVersion(SPOTIFY, "expiredKey", 1)).isEmpty();
        assertThat(llmCacheRepository.findByRegionAndKeyAndVersion(SPOTIFY, "activeKey", 1)).isNotEmpty();
    }

    @Test
    public void shouldClearRegion() {

        llmCacheService.put(SPOTIFY, "someKey", 1, "someValue");

        assertThat(llmCacheService.clear(SPOTIFY)).isEqualTo(1);

        assertThat(llmCacheRepository.count()).isZero();
    }

    @Test
    public void shouldClearAll() {

        llmCacheService.put(SPOTIFY, "someKey", 1, "someValue");

        assertThat(llmCacheService.clear()).isEqualTo(1);

        assertThat(llmCacheRepository.count()).isZero();
    }
}
