package net.dorokhov.pony2.api.library.domain;

import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.Optional;

public enum DiscoveryTaskType {
    SPOTIFY_ARTIST_DATA(true),
    SPOTIFY_TOP_TRACKS(true, Instant.parse("2026-10-27T13:36:25Z")),
    MUSICBRAINZ_ALBUM_DATA(false, Instant.parse("2026-10-28T15:46:56Z"));

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
