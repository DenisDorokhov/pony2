package net.dorokhov.pony2.api.library.domain;

import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.Optional;

public enum DiscoveryTaskType {
    SPOTIFY_ARTIST_DATA(true),
    SPOTIFY_TOP_TRACKS(true, LocalDateTime.of(2026, 10, 27, 14, 36, 25)),
    MUSICBRAINZ_ALBUM_DATA(false, LocalDateTime.of(2026, 10, 27, 14, 36, 25));

    private final boolean toolsEnabled;
    private final LocalDateTime implementedAt;

    DiscoveryTaskType(boolean toolsEnabled) {
        this(toolsEnabled, null);
    }

    DiscoveryTaskType(boolean toolsEnabled, @Nullable LocalDateTime implementedAt) {
        this.toolsEnabled = toolsEnabled;
        this.implementedAt = implementedAt;
    }

    public boolean isToolsEnabled() {
        return toolsEnabled;
    }

    public Optional<LocalDateTime> getImplementedAt() {
        return Optional.ofNullable(implementedAt);
    }
}
