package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.AlbumDiscoveryData;
import net.dorokhov.pony2.api.library.domain.AlbumDiscoveryData.ReleaseType;

import java.time.LocalDateTime;

public final class AlbumDiscoveryDto {

    private String musicBrainzReleaseGroupId;
    private ReleaseType releaseType;
    private LocalDateTime releaseDate;
    private ReleaseType overrideReleaseType;

    public @Nullable String getMusicBrainzReleaseGroupId() {
        return musicBrainzReleaseGroupId;
    }

    public AlbumDiscoveryDto setMusicBrainzReleaseGroupId(@Nullable String musicBrainzReleaseGroupId) {
        this.musicBrainzReleaseGroupId = musicBrainzReleaseGroupId;
        return this;
    }

    public ReleaseType getReleaseType() {
        return releaseType;
    }

    public AlbumDiscoveryDto setReleaseType(ReleaseType releaseType) {
        this.releaseType = releaseType;
        return this;
    }

    public @Nullable LocalDateTime getReleaseDate() {
        return releaseDate;
    }

    public AlbumDiscoveryDto setReleaseDate(@Nullable LocalDateTime releaseDate) {
        this.releaseDate = releaseDate;
        return this;
    }

    public @Nullable ReleaseType getOverrideReleaseType() {
        return overrideReleaseType;
    }

    public AlbumDiscoveryDto setOverrideReleaseType(@Nullable ReleaseType overrideReleaseType) {
        this.overrideReleaseType = overrideReleaseType;
        return this;
    }

    public static AlbumDiscoveryDto of(@Nullable AlbumDiscoveryData data) {
        return new AlbumDiscoveryDto()
                .setMusicBrainzReleaseGroupId(data != null ? data.musicBrainzReleaseGroupId() : null)
                .setReleaseType(data != null ? data.releaseType() : ReleaseType.UNKNOWN)
                .setReleaseDate(data != null ? data.releaseDate() : null)
                .setOverrideReleaseType(data != null ? data.overrideReleaseType() : null);
    }
}
