package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.api.library.domain.Song;

import static com.google.common.base.Preconditions.checkNotNull;

public final class SongDetailsDto {

    private final SongDto song;
    private final AlbumDetailsDto albumDetails;
    private final GenreDto genre;

    private SongDetailsDto(SongDto song, AlbumDetailsDto albumDetails, GenreDto genre) {
        this.song = checkNotNull(song);
        this.albumDetails = checkNotNull(albumDetails);
        this.genre = checkNotNull(genre);
    }

    public SongDto getSong() {
        return song;
    }

    public AlbumDetailsDto getAlbumDetails() {
        return albumDetails;
    }

    public GenreDto getGenre() {
        return genre;
    }

    public static SongDetailsDto of(Song song) {
        return new SongDetailsDto(SongDto.of(song), AlbumDetailsDto.of(song.getAlbum()), GenreDto.of(song.getGenre()));
    }
}
