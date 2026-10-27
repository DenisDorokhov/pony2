package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.AlbumDiscoveryData;

public final class AlbumDetailsDto {

    private AlbumDto album;
    private AlbumDiscoveryDataDto discoveryData;

    public AlbumDto getAlbum() {
        return album;
    }

    public AlbumDetailsDto setAlbum(AlbumDto album) {
        this.album = album;
        return this;
    }

    public @Nullable AlbumDiscoveryDataDto getDiscoveryData() {
        return discoveryData;
    }

    public AlbumDetailsDto setDiscoveryData(@Nullable AlbumDiscoveryDataDto discoveryData) {
        this.discoveryData = discoveryData;
        return this;
    }

    public static AlbumDetailsDto of(Album album, @Nullable AlbumDiscoveryData discoveryData) {
        return new AlbumDetailsDto()
                .setAlbum(AlbumDto.of(album))
                .setDiscoveryData(discoveryData != null ? AlbumDiscoveryDataDto.of(discoveryData) : null);
    }
}
