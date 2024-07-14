package net.dorokhov.pony3.web.dto;

import net.dorokhov.pony3.api.library.domain.Album;

import java.util.List;

public final class AlbumSongsDto {

    private AlbumDto album;
    private List<SongDto> songs;

    public AlbumDto getAlbum() {
        return album;
    }

    public AlbumSongsDto setAlbum(AlbumDto album) {
        this.album = album;
        return this;
    }

    public List<SongDto> getSongs() {
        return songs;
    }

    public AlbumSongsDto setSongs(List<SongDto> songs) {
        this.songs = songs;
        return this;
    }

    public static AlbumSongsDto of(Album album) {
        return new AlbumSongsDto()
                .setAlbum(AlbumDto.of(album))
                .setSongs(album.getSongs().stream()
                        .sorted()
                        .map(SongDto::of)
                        .toList());
    }
}
