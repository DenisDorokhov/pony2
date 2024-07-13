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
@RequestMapping(value = "/api/library", produces = APPLICATION_JSON_VALUE)
public class LibraryController implements ErrorHandlingController {

    private final static int MAX_RANDOM_COUNT = 30;

    private final LibraryFacade libraryFacade;
    private final ScanFacade scanFacade;

    public LibraryController(LibraryFacade libraryFacade, ScanFacade scanFacade) {
        this.libraryFacade = libraryFacade;
        this.scanFacade = scanFacade;
    }
    
    @GetMapping("/song/{songIds}")
    public List<SongDetailsDto> getSong(@PathVariable List<String> songIds) {
        return libraryFacade.getSongs(songIds);
    }

    @GetMapping("/artists")
    public List<ArtistDto> getArtists() {
        return libraryFacade.getArtists();
    }

    @GetMapping("/artistSongs/{artistId}")
    public ArtistSongsDto getArtistSongs(@PathVariable String artistId) throws ObjectNotFoundException {
        return libraryFacade.getArtistSongs(artistId);
    }

    @GetMapping("/genres")
    public List<GenreDto> getGenres() {
        return libraryFacade.getGenres();
    }

    @GetMapping("/genreSongs/{genreId}")
    public GenreSongsPageDto getGenreSongs(@PathVariable String genreId, @RequestParam(defaultValue = "0") int pageIndex) throws ObjectNotFoundException {
        return libraryFacade.getGenreSongs(genreId, pageIndex);
    }

    @GetMapping("/randomSongs")
    public List<SongDetailsDto> getRandomSongs(@RequestParam(defaultValue = "10") int count) {
        return libraryFacade.getRandomSongs(Math.min(count, MAX_RANDOM_COUNT));
    }

    @GetMapping("/randomArtistSongs/{artistId}")
    public List<SongDetailsDto> getArtistRandomSongs(@PathVariable String artistId, @RequestParam(defaultValue = "10") int count) {
        return libraryFacade.getRandomSongsByArtistId(artistId, Math.min(count, MAX_RANDOM_COUNT));
    }

    @GetMapping("/randomAlbumSongs/{albumId}")
    public List<SongDetailsDto> getAlbumRandomSongs(@PathVariable String albumId, @RequestParam(defaultValue = "10") int count) {
        return libraryFacade.getRandomSongsByAlbumId(albumId, Math.min(count, MAX_RANDOM_COUNT));
    }

    @GetMapping("/randomGenreSongs/{genreId}")
    public List<SongDetailsDto> getGenreRandomSongs(@PathVariable String genreId, @RequestParam(defaultValue = "10") int count) {
        return libraryFacade.getRandomSongsByGenreId(genreId, Math.min(count, MAX_RANDOM_COUNT));
    }

    @GetMapping("/scanStatus")
    public ScanStatusDto getScanStatus() {
        return scanFacade.getScanStatus();
    }

    @GetMapping("/scanStatistics")
    public ScanStatisticsDto getStatistics() throws ObjectNotFoundException {
        return scanFacade.getScanStatistics();
    }
}
