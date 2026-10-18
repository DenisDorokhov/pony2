package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.AlbumDiscoveryData;
import net.dorokhov.pony2.api.library.domain.ArtistDiscoveryData;

public final class AlbumDetailsDto {

    private AlbumDto album;
    private ArtistDto artist;

    public AlbumDto getAlbum() {
        return album;
    }

    public AlbumDetailsDto setAlbum(AlbumDto album) {
        this.album = album;
        return this;
    }

    public ArtistDto getArtist() {
        return artist;
    }

    public AlbumDetailsDto setArtist(ArtistDto artist) {
        this.artist = artist;
        return this;
    }

    public static AlbumDetailsDto of(Album album, ArtistDiscoveryData artistDiscoveryData, AlbumDiscoveryData albumDiscoveryData) {
        return new AlbumDetailsDto()
                .setAlbum(AlbumDto.of(album, albumDiscoveryData))
                .setArtist(ArtistDto.of(album.getArtist(), artistDiscoveryData));
    }
}
