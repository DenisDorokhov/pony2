package net.dorokhov.pony.web.service.impl;

import net.dorokhov.pony.library.domain.Album;
import net.dorokhov.pony.library.domain.Artist;
import net.dorokhov.pony.library.domain.Genre;
import net.dorokhov.pony.library.domain.Song;
import net.dorokhov.pony.library.service.LibraryService;
import net.dorokhov.pony.library.domain.LibrarySearchQuery;
import net.dorokhov.pony.library.service.LibrarySearchService;
import net.dorokhov.pony.web.domain.*;
import net.dorokhov.pony.web.service.LibraryFacade;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LibraryFacadeImpl implements LibraryFacade {

    private final static int SEARCH_RESULT_COUNT = 10;
    
    private final LibraryService libraryService;
    private final LibrarySearchService librarySearchService;

    public LibraryFacadeImpl(LibraryService libraryService,
                             LibrarySearchService librarySearchService) {
        this.libraryService = libraryService;
        this.librarySearchService = librarySearchService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistDto> getArtists() {
        return libraryService.getArtists().stream()
                .sorted()
                .map(ArtistDto::of)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ArtistSongsDto getArtistSongs(Long artistId) throws ObjectNotFoundException {
        Artist artist = libraryService.getArtistById(artistId);
        if (artist == null) {
            throw new ObjectNotFoundException(Artist.class, artistId);
        }
        return ArtistSongsDto.of(artist);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GenreDto> getGenres() {
        return libraryService.getGenres().stream()
                .sorted()
                .map(GenreDto::of)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public GenreSongsPageDto getGenreSongs(Long genreId, int pageIndex) throws ObjectNotFoundException {
        Genre genre = libraryService.getGenreById(genreId);
        if (genre == null) {
            throw new ObjectNotFoundException(Genre.class, genreId);
        }
        return GenreSongsPageDto.of(genre, libraryService.getSongsByGenreId(genreId, pageIndex));
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResultDto search(String query) {
        List<Genre> genres = librarySearchService.searchGenres(LibrarySearchQuery.of(query), SEARCH_RESULT_COUNT);
        List<Artist> artists = librarySearchService.searchArtists(LibrarySearchQuery.of(query), SEARCH_RESULT_COUNT);
        List<Album> albums = librarySearchService.searchAlbums(LibrarySearchQuery.of(query), SEARCH_RESULT_COUNT);
        List<Song> songs = librarySearchService.searchSongs(LibrarySearchQuery.of(query), SEARCH_RESULT_COUNT);
        return SearchResultDto.of(genres, artists, albums, songs);
    }
}
