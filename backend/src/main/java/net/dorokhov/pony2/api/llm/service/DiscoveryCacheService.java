package net.dorokhov.pony2.api.llm.service;

import net.dorokhov.pony2.api.llm.domain.DiscoveryCache;
import net.dorokhov.pony2.api.llm.domain.DiscoveryCacheRegion;

import java.util.Optional;
import java.util.function.Supplier;

public interface DiscoveryCacheService {

    Optional<String> get(DiscoveryCacheRegion region, String key, int version);

    DiscoveryCache put(DiscoveryCacheRegion region, String key, int version, String value);

    String getOrPut(DiscoveryCacheRegion region, String key, int version, Supplier<String> valueSupplier);

    long deleteExpired();

    long clear();

    long clear(DiscoveryCacheRegion region);
}
