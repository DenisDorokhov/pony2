package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.Album;

import java.util.List;

public final class AlbumSongDetailsDto {

    private AlbumDto album;
    private List<SongDetailsDto> songs;

    public AlbumDto getAlbum() {
        return album;
    }

    public AlbumSongDetailsDto setAlbum(AlbumDto album) {
        this.album = album;
        return this;
    }

    public List<SongDetailsDto> getSongs() {
        return songs;
    }

    public AlbumSongDetailsDto setSongs(List<SongDetailsDto> songs) {
        this.songs = songs;
        return this;
    }

    public static AlbumSongDetailsDto of(Album album, boolean isAdmin) {
        return new AlbumSongDetailsDto()
                .setAlbum(AlbumDto.of(album))
                .setSongs(album.getSongs().stream()
                        .sorted()
                        .map(song -> SongDetailsDto.of(song, isAdmin))
                        .toList());
    }
}
