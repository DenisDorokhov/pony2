package net.dorokhov.pony2.web.controller;

import jakarta.validation.Valid;
import net.dorokhov.pony2.web.controller.common.ErrorHandlingController;
import net.dorokhov.pony2.web.dto.*;
import net.dorokhov.pony2.web.service.LibraryFacade;
import net.dorokhov.pony2.web.service.ScanFacade;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(produces = APPLICATION_JSON_VALUE)
public class LibraryController implements ErrorHandlingController {

    private final LibraryFacade libraryFacade;
    private final ScanFacade scanFacade;

    public LibraryController(LibraryFacade libraryFacade, ScanFacade scanFacade) {
        this.libraryFacade = libraryFacade;
        this.scanFacade = scanFacade;
    }
    
    @PostMapping("/api/library/songs/fetchByIds")
    public List<SongDetailsDto> getSongs(@RequestBody List<String> songIds) {
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

    @PostMapping("/api/library/fetchRandomSongs")
    public List<SongDetailsDto> getGenreRandomSongs(@Valid @RequestBody RandomSongsRequestDto request) {
        return libraryFacade.getRandomSongs(request);
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
