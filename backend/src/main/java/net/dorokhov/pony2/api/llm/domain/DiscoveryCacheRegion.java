package net.dorokhov.pony2.api.llm.domain;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public enum DiscoveryCacheRegion {

    SPOTIFY(Duration.of(30, ChronoUnit.DAYS).getSeconds()),
    MUSICBRAINZ(Duration.of(30, ChronoUnit.DAYS).getSeconds()),
    ;

    private final long expirationInSeconds;

    DiscoveryCacheRegion(long expirationInSeconds) {
        this.expirationInSeconds = expirationInSeconds;
    }

    public long getExpirationInSeconds() {
        return expirationInSeconds;
    }
}
