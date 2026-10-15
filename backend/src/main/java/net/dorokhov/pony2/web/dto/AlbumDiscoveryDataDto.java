package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.AlbumDiscoveryData;
import net.dorokhov.pony2.api.library.domain.AlbumDiscoveryData.ReleaseType;

public final class AlbumDiscoveryDataDto {

    private ReleaseType releaseType;

    public ReleaseType getReleaseType() {
        return releaseType;
    }

    public AlbumDiscoveryDataDto setReleaseType(ReleaseType releaseType) {
        this.releaseType = releaseType;
        return this;
    }

    public static AlbumDiscoveryDataDto of(AlbumDiscoveryData data) {
        return new AlbumDiscoveryDataDto().setReleaseType(data.releaseType());
    }
}
