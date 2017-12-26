package net.dorokhov.pony.web.controller;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.dorokhov.pony.web.domain.ArtistDto;
import net.dorokhov.pony.web.domain.ArtistSongsDto;
import net.dorokhov.pony.web.domain.GenreDto;
import net.dorokhov.pony.web.domain.GenreSongsPageDto;
import net.dorokhov.pony.web.domain.ScanStatisticsDto;
import net.dorokhov.pony.web.domain.ScanStatusDto;
import net.dorokhov.pony.web.domain.SearchResultDto;
import net.dorokhov.pony.web.service.LibraryFacade;
import net.dorokhov.pony.web.service.ScanFacade;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/library", produces = "application/json")
@Api(tags = "Library")
public class LibraryController implements ErrorHandlingController {

    private final LibraryFacade libraryFacade;
    private final ScanFacade scanFacade;

    public LibraryController(LibraryFacade libraryFacade, ScanFacade scanFacade) {
        this.libraryFacade = libraryFacade;
        this.scanFacade = scanFacade;
    }

    @GetMapping("/artists")
    @ApiOperation("Get list of artists.")
    public List<ArtistDto> getArtists() {
        return libraryFacade.getArtists();
    }

    @GetMapping("/artistSongs/{artistId}")
    @ApiOperation("Get artist songs by artist ID.")
    public ArtistSongsDto getArtistSongs(@PathVariable Long artistId) throws ObjectNotFoundException {
        return libraryFacade.getArtistSongs(artistId);
    }

    @GetMapping("/genres")
    @ApiOperation("Get list of genres.")
    public List<GenreDto> getGenres() {
        return libraryFacade.getGenres();
    }

    @GetMapping("/genreSongs/{genreId}")
    @ApiOperation("Get page of genre songs by genre ID.")
    public GenreSongsPageDto getGenreSongs(@PathVariable Long genreId, @RequestParam(defaultValue = "0") int pageIndex) throws ObjectNotFoundException {
        return libraryFacade.getGenreSongs(genreId, pageIndex);
    }

    @GetMapping("/search")
    @ApiOperation("Search genres, artists, albums and songs.")
    public SearchResultDto search(@RequestParam String query) {
        return libraryFacade.search(query);
    }

    @GetMapping("/scanStatus")
    @ApiOperation("Get status for currently running scan.")
    public ScanStatusDto getScanStatus() {
        return scanFacade.getScanStatus();
    }

    @GetMapping("/scanStatistics")
    @ApiOperation("Get last scan statistics.")
    public ScanStatisticsDto getStatistics() throws ObjectNotFoundException {
        return scanFacade.getScanStatistics();
    }
}
