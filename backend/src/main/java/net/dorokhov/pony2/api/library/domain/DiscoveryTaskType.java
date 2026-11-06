package net.dorokhov.pony2.api.library.domain;

import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.Optional;

public enum DiscoveryTaskType {
    SPOTIFY_ARTIST_DATA(true),
    SPOTIFY_TOP_TRACKS(true),
    MUSICBRAINZ_ALBUM_DATA(false, Instant.parse("2026-11-07T00:00:00Z")),
    ARTIST_AVATAR_IMAGE(false),
    ARTIST_BIO_IMAGE(false),
    ARTIST_BIO_IMAGE_FACES(false),
    MUSICBRAINZ_ARTIST_DATA(false),
    ;

    private final boolean toolsEnabled;
    private final Instant implementedAt;

    DiscoveryTaskType(boolean toolsEnabled) {
        this(toolsEnabled, null);
    }

    DiscoveryTaskType(boolean toolsEnabled, @Nullable Instant implementedAt) {
        this.toolsEnabled = toolsEnabled;
        this.implementedAt = implementedAt;
    }

    public boolean isToolsEnabled() {
        return toolsEnabled;
    }

    public Optional<Instant> getImplementedAt() {
        return Optional.ofNullable(implementedAt);
    }
}
