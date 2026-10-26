package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.AlbumDiscoveryData;

import java.util.List;

public final class AlbumSongsDto {

    private AlbumDetailsDto album;
    private List<SongDto> songs;

    public AlbumDetailsDto getAlbum() {
        return album;
    }

    public AlbumSongsDto setAlbum(AlbumDetailsDto album) {
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

    public static AlbumSongsDto of(Album album, @Nullable AlbumDiscoveryData albumDiscoveryData, boolean isAdmin) {
        return new AlbumSongsDto()
                .setAlbum(AlbumDetailsDto.of(album, albumDiscoveryData))
                .setSongs(album.getSongs().stream()
                        .sorted()
                        .map(song -> SongDto.of(song, isAdmin))
                        .toList());
    }
}
