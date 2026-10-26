package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.Album;

public final class ArtistAlbumDto {

    private ArtistDto artist;
    private AlbumDto album;

    public ArtistDto getArtist() {
        return artist;
    }

    public ArtistAlbumDto setArtist(ArtistDto artist) {
        this.artist = artist;
        return this;
    }

    public AlbumDto getAlbum() {
        return album;
    }

    public ArtistAlbumDto setAlbum(AlbumDto album) {
        this.album = album;
        return this;
    }

    public static ArtistAlbumDto of(Album album) {
        return new ArtistAlbumDto()
                .setArtist(ArtistDto.of(album.getArtist()))
                .setAlbum(AlbumDto.of(album));
    }
}
