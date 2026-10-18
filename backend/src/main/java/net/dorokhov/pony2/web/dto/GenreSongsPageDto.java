package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Genre;
import net.dorokhov.pony2.api.library.domain.Song;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public class GenreSongsPageDto extends PageDto<GenreSongsPageDto> {

    private GenreDto genre;
    private List<SongAlbumDto> songAlbums;

    public GenreDto getGenre() {
        return genre;
    }

    public GenreSongsPageDto setGenre(GenreDto genre) {
        this.genre = genre;
        return this;
    }

    public List<SongAlbumDto> getSongAlbums() {
        return songAlbums;
    }

    public GenreSongsPageDto setSongAlbums(List<SongAlbumDto> songAlbums) {
        this.songAlbums = songAlbums;
        return this;
    }

    public static GenreSongsPageDto of(Genre genre, Page<Song> songPage, Function<Album, AlbumDetailsDto> albumMapper, boolean isAdmin) {
        return new GenreSongsPageDto()
                .setPageIndex(songPage.getNumber())
                .setPageSize(songPage.getSize())
                .setTotalPages(songPage.getTotalPages())
                .setGenre(GenreDto.of(genre))
                .setSongAlbums(songPage.getContent().stream()
                        .map(songs -> SongAlbumDto.of(songs, albumMapper, isAdmin))
                        .toList());
    }
}
