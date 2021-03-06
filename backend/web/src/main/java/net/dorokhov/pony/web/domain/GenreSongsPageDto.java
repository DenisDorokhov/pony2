package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.api.library.domain.Genre;
import net.dorokhov.pony.api.library.domain.Song;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.unmodifiableList;

public class GenreSongsPageDto extends PageDto {

    private final GenreDto genre;
    private final List<SongAlbumDto> songAlbums;

    private GenreSongsPageDto(int pageIndex, int pageSize, int totalPages,
                              GenreDto genre, List<SongAlbumDto> songAlbums) {
        super(pageIndex, pageSize, totalPages);
        this.genre = checkNotNull(genre);
        this.songAlbums = unmodifiableList(songAlbums);
    }

    public GenreDto getGenre() {
        return genre;
    }

    public List<SongAlbumDto> getSongAlbums() {
        return songAlbums;
    }

    public static GenreSongsPageDto of(Genre genre, Page<Song> songPage) {
        return new GenreSongsPageDto(songPage.getNumber(), songPage.getSize(), songPage.getTotalPages(), GenreDto.of(genre),
                songPage.getContent().stream()
                        .map(SongAlbumDto::of)
                        .collect(Collectors.toList()));
    }
}
