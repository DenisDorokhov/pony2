package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import net.dorokhov.pony2.api.library.domain.AlbumDiscoveryData.ReleaseType;

public final class AlbumDiscoveryUpdateCommandDto {

    @NotBlank
    private String albumId;

    private ReleaseType overrideReleaseType;

    public String getAlbumId() {
        return albumId;
    }

    public AlbumDiscoveryUpdateCommandDto setAlbumId(String albumId) {
        this.albumId = albumId;
        return this;
    }

    public @Nullable ReleaseType getOverrideReleaseType() {
        return overrideReleaseType;
    }

    public AlbumDiscoveryUpdateCommandDto setOverrideReleaseType(@Nullable ReleaseType overrideReleaseType) {
        this.overrideReleaseType = overrideReleaseType;
        return this;
    }
}
