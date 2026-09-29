package net.dorokhov.pony2.api.llm.service;

import net.dorokhov.pony2.api.llm.LlmCache;
import net.dorokhov.pony2.api.llm.LlmCacheRegion;

import java.util.Optional;
import java.util.function.Supplier;

public interface LlmCacheService {

    Optional<String> get(LlmCacheRegion region, String key, int version);

    LlmCache put(LlmCacheRegion region, String key, int version, String value);

    String getOrPut(LlmCacheRegion region, String key, int version, Supplier<String> valueSupplier);

    long deleteExpired();

    long clear();

    long clear(LlmCacheRegion region);
}
