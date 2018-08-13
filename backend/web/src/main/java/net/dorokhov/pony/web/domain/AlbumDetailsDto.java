package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.api.library.domain.Album;

import static com.google.common.base.Preconditions.checkNotNull;

public final class AlbumDetailsDto {

    private final AlbumDto album;
    private final ArtistDto artist;

    private AlbumDetailsDto(AlbumDto album, ArtistDto artist) {
        this.album = checkNotNull(album);
        this.artist = checkNotNull(artist);
    }

    public AlbumDto getAlbum() {
        return album;
    }

    public ArtistDto getArtist() {
        return artist;
    }

    public static AlbumDetailsDto of(Album album) {
        return new AlbumDetailsDto(AlbumDto.of(album), ArtistDto.of(album.getArtist()));
    }
}
