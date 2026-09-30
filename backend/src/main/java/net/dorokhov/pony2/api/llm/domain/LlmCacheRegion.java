package net.dorokhov.pony2.api.llm.domain;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public enum LlmCacheRegion {

    SPOTIFY(Duration.of(30, ChronoUnit.DAYS).getSeconds()),
    ;

    private final long expirationInSeconds;

    LlmCacheRegion(long expirationInSeconds) {
        this.expirationInSeconds = expirationInSeconds;
    }

    public long getExpirationInSeconds() {
        return expirationInSeconds;
    }
}
