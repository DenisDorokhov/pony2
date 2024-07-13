package net.dorokhov.pony3.web.service;

import net.dorokhov.pony3.api.library.domain.*;
import net.dorokhov.pony3.api.library.service.LibraryService;
import net.dorokhov.pony3.web.dto.*;
import net.dorokhov.pony3.web.service.exception.ObjectNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
public class LibraryFacade {

    private final static int SEARCH_RESULT_COUNT = 10;
    
    private final LibraryService libraryService;

    public LibraryFacade(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @Transactional(readOnly = true)
    public List<SongDetailsDto> getSongs(List<String> songIds) {
        return libraryService.getSongsByIds(songIds).stream()
                .map(SongDetailsDto::of)
                .collect(toList());
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
        return ArtistSongsDto.of(artist);
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
        return GenreSongsPageDto.of(genre, libraryService.getSongsByGenreId(genreId, pageIndex));
    }

    @Transactional(readOnly = true)
    public List<SongDetailsDto> getRandomSongs(int count) {
        return libraryService.getRandomSongs(count).stream()
                .map(SongDetailsDto::of)
                .collect(toList());
    }

    @Transactional(readOnly = true)
    public List<SongDetailsDto> getRandomSongsByAlbumId(String albumId, int count) {
        return libraryService.getRandomSongsByAlbumId(albumId, count).stream()
                .map(SongDetailsDto::of)
                .collect(toList());
    }

    @Transactional(readOnly = true)
    public List<SongDetailsDto> getRandomSongsByArtistId(String artistId, int count) {
        return libraryService.getRandomSongsByArtistId(artistId, count).stream()
                .map(SongDetailsDto::of)
                .collect(toList());
    }

    @Transactional(readOnly = true)
    public List<SongDetailsDto> getRandomSongsByGenreId(String genreId, int count) {
        return libraryService.getRandomSongsByGenreId(genreId, count).stream()
                .map(SongDetailsDto::of)
                .collect(toList());
    }
}
