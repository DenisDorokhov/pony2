package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.Album;

import java.util.List;

public final class AlbumSongDetailsDto {

    private AlbumDetailsDto details;
    private List<SongDetailsDto> songs;

    public AlbumDetailsDto getDetails() {
        return details;
    }

    public AlbumSongDetailsDto setDetails(AlbumDetailsDto details) {
        this.details = details;
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
                .setDetails(AlbumDetailsDto.of(album))
                .setSongs(album.getSongs().stream()
                        .sorted()
                        .map(song -> SongDetailsDto.of(song, isAdmin))
                        .toList());
    }
}
