package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.api.library.domain.Song;

import static com.google.common.base.Preconditions.checkNotNull;

public final class SongAlbumDto {

    private final SongDto song;
    private final AlbumDetailsDto albumDetails;

    private SongAlbumDto(SongDto song, AlbumDetailsDto albumDetails) {
        this.song = checkNotNull(song);
        this.albumDetails = checkNotNull(albumDetails);
    }

    public SongDto getSong() {
        return song;
    }

    public AlbumDetailsDto getAlbumDetails() {
        return albumDetails;
    }

    public static SongAlbumDto of(Song song) {
        return new SongAlbumDto(SongDto.of(song), AlbumDetailsDto.of(song.getAlbum()));
    }
}
