package net.dorokhov.pony2.api.library.domain;

import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

public record AlbumDiscoveryData(
        ReleaseType releaseType,
        @Nullable LocalDateTime releaseDate,
        @Nullable ReleaseType overrideReleaseType,
        @Nullable String musicBrainzReleaseGroupId
) {
    public enum ReleaseType {
        LP, EP, SINGLE, LIVE, COMPILATION, UNKNOWN
    }
}
