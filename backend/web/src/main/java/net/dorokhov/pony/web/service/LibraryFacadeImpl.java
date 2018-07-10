package net.dorokhov.pony.web.service;

import net.dorokhov.pony.api.library.domain.*;
import net.dorokhov.pony.api.library.service.LibrarySearchService;
import net.dorokhov.pony.api.library.service.LibraryService;
import net.dorokhov.pony.web.domain.*;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.stream.Collectors.toList;

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
    public List<SongDetailsDto> getSongs(List<Long> songIds) {
        return libraryService.getSongsByIds(songIds).stream()
                .map(SongDetailsDto::of)
                .collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistDto> getArtists() {
        return libraryService.getArtists().stream()
                .sorted()
                .map(ArtistDto::of)
                .collect(toList());
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
                .collect(toList());
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

    @Override
    @Transactional(readOnly = true)
    public List<SongDetailsDto> getRandomSongs(int count) {
        return libraryService.getRandomSongs(count).stream()
                .map(SongDetailsDto::of)
                .collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SongDetailsDto> getRandomSongsByAlbumId(Long albumId, int count) {
        return libraryService.getRandomSongsByAlbumId(albumId, count).stream()
                .map(SongDetailsDto::of)
                .collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SongDetailsDto> getRandomSongsByArtistId(Long artistId, int count) {
        return libraryService.getRandomSongsByArtistId(artistId, count).stream()
                .map(SongDetailsDto::of)
                .collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SongDetailsDto> getRandomSongsByGenreId(Long genreId, int count) {
        return libraryService.getRandomSongsByGenreId(genreId, count).stream()
                .map(SongDetailsDto::of)
                .collect(toList());
    }
}
