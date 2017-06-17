package net.dorokhov.pony.web.service.impl;

import net.dorokhov.pony.library.domain.*;
import net.dorokhov.pony.library.service.LibraryService;
import net.dorokhov.pony.library.service.ScanJobService;
import net.dorokhov.pony.search.domain.SearchQuery;
import net.dorokhov.pony.search.service.SearchService;
import net.dorokhov.pony.web.controller.exception.ObjectNotFoundException;
import net.dorokhov.pony.web.domain.*;
import net.dorokhov.pony.web.service.LibraryFacade;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LibraryFacadeImpl implements LibraryFacade {

    private final static int SEARCH_RESULT_COUNT = 10;
    
    private final LibraryService libraryService;
    private final SearchService searchService;
    private final ScanJobService scanJobService;

    public LibraryFacadeImpl(LibraryService libraryService, 
                             SearchService searchService, 
                             ScanJobService scanJobService) {
        this.libraryService = libraryService;
        this.searchService = searchService;
        this.scanJobService = scanJobService;
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
    public GenreSongsDto getGenreSongs(Long genreId, int pageIndex) throws ObjectNotFoundException {
        Genre genre = libraryService.getGenreById(genreId);
        if (genre == null) {
            throw new ObjectNotFoundException(Genre.class, genreId);
        }
        Page<Song> songPage = libraryService.getSongsByGenreId(genreId, pageIndex);
        return new GenreSongsDto(pageIndex, songPage.getSize(), songPage.getTotalPages(), GenreDto.of(genre),
                songPage.getContent().stream()
                        .map(SongAlbumDto::of)
                        .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResultDto search(String query) {
        List<Genre> genres = searchService.searchGenres(SearchQuery.of(query), SEARCH_RESULT_COUNT);
        List<Artist> artists = searchService.searchArtists(SearchQuery.of(query), SEARCH_RESULT_COUNT);
        List<Album> albums = searchService.searchAlbums(SearchQuery.of(query), SEARCH_RESULT_COUNT);
        List<Song> songs = searchService.searchSongs(SearchQuery.of(query), SEARCH_RESULT_COUNT);
        return SearchResultDto.of(genres, artists, albums, songs);
    }

    @Override
    @Transactional(readOnly = true)
    public ScanStatusDto getScanStatus() {
        ScanJobProgress scanJobProgress = scanJobService.getCurrentScanJobProgress();
        return new ScanStatusDto(scanJobProgress != null);
    }
}
