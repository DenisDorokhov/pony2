package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.api.library.domain.Song;

import static com.google.common.base.Preconditions.checkNotNull;

public final class SongAlbumDto {

    private final SongDto song;
    private final AlbumDetailsDto album;

    SongAlbumDto(SongDto song, AlbumDetailsDto album) {
        this.song = checkNotNull(song);
        this.album = checkNotNull(album);
    }

    public SongDto getSong() {
        return song;
    }

    public AlbumDetailsDto getAlbum() {
        return album;
    }

    public static SongAlbumDto of(Song song) {
        return new SongAlbumDto(SongDto.of(song), AlbumDetailsDto.of(song.getAlbum()));
    }
}
