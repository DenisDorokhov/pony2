package net.dorokhov.pony2.core.library.service;

import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.library.domain.DiscoveryCache;
import net.dorokhov.pony2.api.library.service.DiscoveryCacheService;
import net.dorokhov.pony2.core.library.repository.DiscoveryCacheRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static net.dorokhov.pony2.api.library.domain.DiscoveryCacheRegion.SPOTIFY;
import static org.assertj.core.api.Assertions.assertThat;

public class DiscoveryCacheServiceImplTest extends IntegrationTest {

    @Autowired
    private DiscoveryCacheService discoveryCacheService;

    @Autowired
    private DiscoveryCacheRepository discoveryCacheRepository;

    @Test
    public void shouldCacheValue() {

        DiscoveryCache cache = discoveryCacheService.put(SPOTIFY, "someKey", 1, "someValue");

        assertThat(cache.getId()).isNotNull();
        assertThat(cache.getRegion()).isSameAs(SPOTIFY);
        assertThat(cache.getKey()).isEqualTo("someKey");
        assertThat(cache.getVersion()).isEqualTo(1);
        assertThat(cache.getValue()).isEqualTo("someValue");
        assertThat(cache.getCreationDate()).isNotNull();
        assertThat(cache.getExpirationDate()).isAfter(cache.getCreationDate());
        assertThat(discoveryCacheService.get(SPOTIFY, "someKey", 1)).contains("someValue");
    }

    @Test
    public void shouldUpdateCachedValue() {

        discoveryCacheService.put(SPOTIFY, "someKey", 1, "someValue");
        discoveryCacheService.put(SPOTIFY, "someKey", 1, "someNewValue");

        assertThat(discoveryCacheRepository.count()).isEqualTo(1);
        assertThat(discoveryCacheService.get(SPOTIFY, "someKey", 1)).contains("someNewValue");
    }

    @Test
    public void shouldNotReturnExpiredValue() {

        DiscoveryCache cache = discoveryCacheService.put(SPOTIFY, "someKey", 1, "someValue");
        discoveryCacheRepository.saveAndFlush(cache.setExpirationDate(LocalDateTime.now().minusSeconds(1)));

        assertThat(discoveryCacheService.get(SPOTIFY, "someKey", 1)).isEmpty();
    }

    @Test
    public void shouldGetOrPutValue() {

        AtomicInteger supplierCalls = new AtomicInteger();

        String firstValue = discoveryCacheService.getOrPut(SPOTIFY, "someKey", 1, () -> {
            supplierCalls.incrementAndGet();
            return "someValue";
        });
        String secondValue = discoveryCacheService.getOrPut(SPOTIFY, "someKey", 1, () -> {
            supplierCalls.incrementAndGet();
            return "someNewValue";
        });

        assertThat(firstValue).isEqualTo("someValue");
        assertThat(secondValue).isEqualTo("someValue");
        assertThat(supplierCalls).hasValue(1);
    }

    @Test
    public void shouldDeleteExpiredValues() {

        DiscoveryCache expiredCache = discoveryCacheService.put(SPOTIFY, "expiredKey", 1, "expiredValue");
        discoveryCacheRepository.saveAndFlush(expiredCache.setExpirationDate(LocalDateTime.now().minusSeconds(1)));
        discoveryCacheService.put(SPOTIFY, "activeKey", 1, "activeValue");

        assertThat(discoveryCacheService.deleteExpired()).isEqualTo(1);

        assertThat(discoveryCacheRepository.findByRegionAndKeyAndVersion(SPOTIFY, "expiredKey", 1)).isEmpty();
        assertThat(discoveryCacheRepository.findByRegionAndKeyAndVersion(SPOTIFY, "activeKey", 1)).isNotEmpty();
    }

    @Test
    public void shouldClearRegion() {

        discoveryCacheService.put(SPOTIFY, "someKey", 1, "someValue");

        assertThat(discoveryCacheService.clear(SPOTIFY)).isEqualTo(1);

        assertThat(discoveryCacheRepository.count()).isZero();
    }

    @Test
    public void shouldClearAll() {

        discoveryCacheService.put(SPOTIFY, "someKey", 1, "someValue");

        assertThat(discoveryCacheService.clear()).isEqualTo(1);

        assertThat(discoveryCacheRepository.count()).isZero();
    }
}
