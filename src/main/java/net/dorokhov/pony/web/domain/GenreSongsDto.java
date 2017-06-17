package net.dorokhov.pony.web.domain;

import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class GenreSongsDto extends PageDto {
    
    private final GenreDto genre;
    private final List<SongAlbumDto> songs;

    public GenreSongsDto(int pageIndex, int pageSize, int totalPages,
                         GenreDto genre, List<SongAlbumDto> songs) {
        super(pageIndex, pageSize, totalPages);
        this.genre = checkNotNull(genre);
        this.songs = ImmutableList.copyOf(songs);
    }

    public GenreDto getGenre() {
        return genre;
    }

    public List<SongAlbumDto> getSongs() {
        return songs;
    }
}
