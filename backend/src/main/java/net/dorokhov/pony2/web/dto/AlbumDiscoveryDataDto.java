package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.AlbumDiscoveryData;
import net.dorokhov.pony2.api.library.domain.AlbumDiscoveryData.ReleaseType;

import java.time.LocalDateTime;

public final class AlbumDiscoveryDataDto {

    private ReleaseType releaseType;
    private ReleaseType overrideReleaseType;
    private LocalDateTime releaseDate;

    public ReleaseType getReleaseType() {
        return releaseType;
    }

    public AlbumDiscoveryDataDto setReleaseType(ReleaseType releaseType) {
        this.releaseType = releaseType;
        return this;
    }

    public @Nullable ReleaseType getOverrideReleaseType() {
        return overrideReleaseType;
    }

    public AlbumDiscoveryDataDto setOverrideReleaseType(@Nullable ReleaseType overrideReleaseType) {
        this.overrideReleaseType = overrideReleaseType;
        return this;
    }

    public @Nullable LocalDateTime getReleaseDate() {
        return releaseDate;
    }

    public AlbumDiscoveryDataDto setReleaseDate(@Nullable LocalDateTime releaseDate) {
        this.releaseDate = releaseDate;
        return this;
    }

    public static AlbumDiscoveryDataDto of(AlbumDiscoveryData data) {
        return new AlbumDiscoveryDataDto()
                .setReleaseType(data.releaseType())
                .setOverrideReleaseType(data.overrideReleaseType())
                .setReleaseDate(data.releaseDate());
    }
}
