package net.dorokhov.pony3.web.dto;

import net.dorokhov.pony3.api.library.domain.Song;

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

    public static SongAlbumDto of(Song song) {
        return new SongAlbumDto()
                .setSong(SongDto.of(song))
                .setAlbumDetails(AlbumDetailsDto.of(song.getAlbum()));
    }
}
