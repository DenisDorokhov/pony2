package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.Song;

public final class SongDetailsDto {

    private SongDto song;
    private AlbumDetailsDto albumDetails;
    private GenreDto genre;

    public SongDto getSong() {
        return song;
    }

    public SongDetailsDto setSong(SongDto song) {
        this.song = song;
        return this;
    }

    public AlbumDetailsDto getAlbumDetails() {
        return albumDetails;
    }

    public SongDetailsDto setAlbumDetails(AlbumDetailsDto albumDetails) {
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

    public static SongDetailsDto of(Song song, boolean isAdmin) {
        return new SongDetailsDto()
                .setSong(SongDto.of(song, isAdmin))
                .setAlbumDetails(AlbumDetailsDto.of(song.getAlbum()))
                .setGenre(GenreDto.of(song.getGenre()));
    }
}
