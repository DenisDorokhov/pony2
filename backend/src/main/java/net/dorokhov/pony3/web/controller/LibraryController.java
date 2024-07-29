package net.dorokhov.pony3.web.controller;

import net.dorokhov.pony3.web.controller.common.ErrorHandlingController;
import net.dorokhov.pony3.web.dto.*;
import net.dorokhov.pony3.web.service.LibraryFacade;
import net.dorokhov.pony3.web.service.ScanFacade;
import net.dorokhov.pony3.web.service.exception.ObjectNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(produces = APPLICATION_JSON_VALUE)
public class LibraryController implements ErrorHandlingController {

    private final static int MAX_RANDOM_COUNT = 30;

    private final LibraryFacade libraryFacade;
    private final ScanFacade scanFacade;

    public LibraryController(LibraryFacade libraryFacade, ScanFacade scanFacade) {
        this.libraryFacade = libraryFacade;
        this.scanFacade = scanFacade;
    }
    
    @GetMapping("/api/library/song/{songIds}")
    public List<SongDetailsDto> getSong(@PathVariable List<String> songIds) {
        return libraryFacade.getSongs(songIds);
    }

    @GetMapping("/api/library/artists")
    public List<ArtistDto> getArtists() {
        return libraryFacade.getArtists();
    }

    @GetMapping("/api/library/artistSongs/{artistId}")
    public ArtistSongsDto getArtistSongs(@PathVariable String artistId) throws ObjectNotFoundException {
        return libraryFacade.getArtistSongs(artistId);
    }

    @GetMapping("/api/library/genres")
    public List<GenreDto> getGenres() {
        return libraryFacade.getGenres();
    }

    @GetMapping("/api/library/genreSongs/{genreId}")
    public GenreSongsPageDto getGenreSongs(@PathVariable String genreId, @RequestParam(defaultValue = "0") int pageIndex) throws ObjectNotFoundException {
        return libraryFacade.getGenreSongs(genreId, pageIndex);
    }

    @GetMapping("/api/library/search")
    public SearchResultDto search(@RequestParam String query) {
        return libraryFacade.search(query);
    }

    @GetMapping("/api/library/randomSongs")
    public List<SongDetailsDto> getRandomSongs(@RequestParam(defaultValue = "10") int count) {
        return libraryFacade.getRandomSongs(Math.min(count, MAX_RANDOM_COUNT));
    }

    @GetMapping("/api/library/randomArtistSongs/{artistId}")
    public List<SongDetailsDto> getArtistRandomSongs(@PathVariable String artistId, @RequestParam(defaultValue = "10") int count) {
        return libraryFacade.getRandomSongsByArtistId(artistId, Math.min(count, MAX_RANDOM_COUNT));
    }

    @GetMapping("/api/library/randomAlbumSongs/{albumId}")
    public List<SongDetailsDto> getAlbumRandomSongs(@PathVariable String albumId, @RequestParam(defaultValue = "10") int count) {
        return libraryFacade.getRandomSongsByAlbumId(albumId, Math.min(count, MAX_RANDOM_COUNT));
    }

    @GetMapping("/api/library/randomGenreSongs/{genreId}")
    public List<SongDetailsDto> getGenreRandomSongs(@PathVariable String genreId, @RequestParam(defaultValue = "10") int count) {
        return libraryFacade.getRandomSongsByGenreId(genreId, Math.min(count, MAX_RANDOM_COUNT));
    }

    @GetMapping("/api/library/scanStatus")
    public ScanStatusDto getScanStatus() {
        return scanFacade.getScanStatus();
    }

    @GetMapping("/api/library/scanStatistics")
    public OptionalResponseDto<ScanStatisticsDto> getStatistics() {
        return scanFacade.getScanStatistics();
    }
}
