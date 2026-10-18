package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Song;

import java.util.function.Function;

public final class SongAlbumDto {

    private SongDto song;
    private AlbumDetailsDto albumDetails;

    public SongDto getSong() {
        return song;
    }

    public SongAlbumDto setSong(SongDto song) {
        this.song = song;
        return this;
    }

    public AlbumDetailsDto getAlbumDetails() {
        return albumDetails;
    }

    public SongAlbumDto setAlbumDetails(AlbumDetailsDto albumDetails) {
        this.albumDetails = albumDetails;
        return this;
    }

    public static SongAlbumDto of(Song song, Function<Album, AlbumDetailsDto> albumMapper, boolean isAdmin) {
        return new SongAlbumDto()
                .setSong(SongDto.of(song, isAdmin))
                .setAlbumDetails(albumMapper.apply(song.getAlbum()));
    }
}
