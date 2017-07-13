package net.dorokhov.pony.web.controller;

import net.dorokhov.pony.web.service.ScanFacade;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;
import net.dorokhov.pony.web.domain.*;
import net.dorokhov.pony.web.service.LibraryFacade;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/library")
public class LibraryController implements ErrorHandlingController {

    private final LibraryFacade libraryFacade;
    private final ScanFacade scanFacade;

    public LibraryController(LibraryFacade libraryFacade, ScanFacade scanFacade) {
        this.libraryFacade = libraryFacade;
        this.scanFacade = scanFacade;
    }

    @GetMapping("/artists")
    public List<ArtistDto> getArtists() {
        return libraryFacade.getArtists();
    }

    @GetMapping("/artistSongs/{artistId}")
    public ArtistSongsDto getArtistSongs(@PathVariable Long artistId) throws ObjectNotFoundException {
        return libraryFacade.getArtistSongs(artistId);
    }

    @GetMapping("/genres")
    public List<GenreDto> getGenres() {
        return libraryFacade.getGenres();
    }

    @GetMapping("/genreSongs/{genreId}")
    public GenreSongsPageDto getGenreSongs(@PathVariable Long genreId, @RequestParam(defaultValue = "0") int pageIndex) throws ObjectNotFoundException {
        return libraryFacade.getGenreSongs(genreId, pageIndex);
    }

    @GetMapping("/search")
    public SearchResultDto search(@RequestParam String query) {
        return libraryFacade.search(query);
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
