package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.library.domain.Album;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.unmodifiableList;

public final class AlbumSongsDto {

    private final AlbumDto album;
    private final List<SongDto> songs;

    AlbumSongsDto(AlbumDto album, List<SongDto> songs) {
        this.album = checkNotNull(album);
        this.songs = unmodifiableList(songs);
    }

    public AlbumDto getAlbum() {
        return album;
    }

    public List<SongDto> getSongs() {
        return songs;
    }

    public static AlbumSongsDto of(Album album) {
        return new AlbumSongsDto(AlbumDto.of(album), album.getSongs().stream()
                .sorted()
                .map(SongDto::of)
                .collect(Collectors.toList()));
    }
}
