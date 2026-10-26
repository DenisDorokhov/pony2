package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.Album;

import java.util.List;
import java.util.function.Function;

public final class AlbumSongDetailsDto {

    private ArtistAlbumDto details;
    private List<SongDetailsDto> songs;

    public ArtistAlbumDto getDetails() {
        return details;
    }

    public AlbumSongDetailsDto setDetails(ArtistAlbumDto details) {
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

    public static AlbumSongDetailsDto of(Album album, Function<Album, ArtistAlbumDto> albumMapper, boolean isAdmin) {
        return new AlbumSongDetailsDto()
                .setDetails(albumMapper.apply(album))
                .setSongs(album.getSongs().stream()
                        .sorted()
                        .map(song -> SongDetailsDto.of(song, albumMapper, isAdmin))
                        .toList());
    }
}
