package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.api.library.domain.Song;

import static com.google.common.base.Preconditions.checkNotNull;

public final class SongDetailsDto {

    private final SongDto song;
    private final AlbumDetailsDto album;
    private final GenreDto genre;

    SongDetailsDto(SongDto song, AlbumDetailsDto album, GenreDto genre) {
        this.song = checkNotNull(song);
        this.album = checkNotNull(album);
        this.genre = checkNotNull(genre);
    }

    public SongDto getSong() {
        return song;
    }

    public AlbumDetailsDto getAlbum() {
        return album;
    }

    public GenreDto getGenre() {
        return genre;
    }

    public static SongDetailsDto of(Song song) {
        return new SongDetailsDto(SongDto.of(song), AlbumDetailsDto.of(song.getAlbum()), GenreDto.of(song.getGenre()));
    }
}
