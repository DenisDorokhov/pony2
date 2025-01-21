package net.dorokhov.pony2.web.service;

import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.library.service.LibrarySearchService;
import net.dorokhov.pony2.api.library.service.LibraryService;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.web.dto.*;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
public class LibraryFacade {

    private final static int SEARCH_RESULT_COUNT = 20;

    private final LibraryService libraryService;
    private final LibrarySearchService librarySearchService;
    private final UserContext userContext;

    public LibraryFacade(
            LibraryService libraryService,
            LibrarySearchService librarySearchService,
            UserContext userContext
    ) {
        this.libraryService = libraryService;
        this.librarySearchService = librarySearchService;
        this.userContext = userContext;
    }

    @Transactional(readOnly = true)
    public List<SongDetailsDto> getSongs(List<String> songIds) {
        return libraryService.getSongsByIds(songIds).stream()
                .map(songs -> SongDetailsDto.of(songs, isAdmin()))
                .collect(toList());
    }

    private boolean isAdmin() {
        return userContext.getAuthenticatedUser().getRoles().contains(User.Role.ADMIN);
    }

    @Transactional(readOnly = true)
    public List<ArtistDto> getArtists() {
        return libraryService.getArtists().stream()
                .sorted()
                .map(ArtistDto::of)
                .collect(toList());
    }

    @Transactional(readOnly = true)
    public ArtistSongsDto getArtistSongs(String artistId) throws ObjectNotFoundException {
        Artist artist = libraryService.getArtistById(artistId).orElse(null);
        if (artist == null) {
            throw new ObjectNotFoundException(Artist.class, artistId);
        }
        return ArtistSongsDto.of(artist, isAdmin());
    }

    @Transactional(readOnly = true)
    public List<GenreDto> getGenres() {
        return libraryService.getGenres().stream()
                .sorted()
                .map(GenreDto::of)
                .collect(toList());
    }

    @Transactional(readOnly = true)
    public GenreSongsPageDto getGenreSongs(String genreId, int pageIndex) throws ObjectNotFoundException {
        Genre genre = libraryService.getGenreById(genreId).orElse(null);
        if (genre == null) {
            throw new ObjectNotFoundException(Genre.class, genreId);
        }
        return GenreSongsPageDto.of(genre, libraryService.getSongsByGenreId(genreId, pageIndex), isAdmin());
    }

    @Transactional(readOnly = true)
    public SearchResultDto search(String query) {
        List<Genre> genres = librarySearchService.searchGenres(LibrarySearchQuery.of(query), SEARCH_RESULT_COUNT);
        List<Artist> artists = librarySearchService.searchArtists(LibrarySearchQuery.of(query), SEARCH_RESULT_COUNT);
        List<Album> albums = librarySearchService.searchAlbums(LibrarySearchQuery.of(query), SEARCH_RESULT_COUNT);
        List<Song> songs = librarySearchService.searchSongs(LibrarySearchQuery.of(query), SEARCH_RESULT_COUNT);
        return SearchResultDto.of(genres, artists, albums, songs, isAdmin());
    }

    @Transactional(readOnly = true)
    public List<SongDetailsDto> getRandomSongs(RandomSongsRequestDto request) {
        return libraryService.getRandomSongs(request.convert()).stream()
                .map(songs -> SongDetailsDto.of(songs, isAdmin()))
                .collect(toList());
    }

    public void reBuildSearchIndexAsync() {
        librarySearchService.reIndexAsync();
    }
}
