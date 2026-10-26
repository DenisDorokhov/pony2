package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Song;

import java.util.function.Function;

public final class SongDetailsDto {

    private SongDto song;
    private ArtistAlbumDto albumDetails;
    private GenreDto genre;

    public SongDto getSong() {
        return song;
    }

    public SongDetailsDto setSong(SongDto song) {
        this.song = song;
        return this;
    }

    public ArtistAlbumDto getAlbumDetails() {
        return albumDetails;
    }

    public SongDetailsDto setAlbumDetails(ArtistAlbumDto albumDetails) {
        this.albumDetails = albumDetails;
        return this;
    }

    public GenreDto getGenre() {
        return genre;
    }

    public SongDetailsDto setGenre(GenreDto genre) {
        this.genre = genre;
        return this;
    }

    public static SongDetailsDto of(Song song, Function<Album, ArtistAlbumDto> albumMapper, boolean isAdmin) {
        return new SongDetailsDto()
                .setSong(SongDto.of(song, isAdmin))
                .setAlbumDetails(albumMapper.apply(song.getAlbum()))
                .setGenre(GenreDto.of(song.getGenre()));
    }
}
