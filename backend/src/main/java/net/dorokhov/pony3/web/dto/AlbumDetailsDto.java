package net.dorokhov.pony3.web.dto;

import net.dorokhov.pony3.api.library.domain.Album;

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

    public static AlbumDetailsDto of(Album album) {
        return new AlbumDetailsDto()
                .setAlbum(AlbumDto.of(album))
                .setArtist(ArtistDto.of(album.getArtist()));
    }
}
