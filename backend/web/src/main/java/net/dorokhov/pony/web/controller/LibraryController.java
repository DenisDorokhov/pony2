package net.dorokhov.pony.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import net.dorokhov.pony.web.controller.common.ErrorHandlingController;
import net.dorokhov.pony.web.domain.*;
import net.dorokhov.pony.web.service.LibraryFacade;
import net.dorokhov.pony.web.service.ScanFacade;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static net.dorokhov.pony.web.controller.common.ApiResponseValues.*;

@RestController
@RequestMapping(value = "/api/library", produces = "application/json")
@Api(tags = "Library")
@ApiResponses({
        @ApiResponse(code = UNAUTHORIZED_CODE, message = UNAUTHORIZED_MESSAGE, response = ErrorDto.class),
})
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
    @ApiResponses({
            @ApiResponse(code = NOT_FOUND_CODE, message = "Requested artist not found.", response = ErrorDto.class),
    })
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
    @ApiResponses({
            @ApiResponse(code = NOT_FOUND_CODE, message = "Requested genre not found.", response = ErrorDto.class),
    })
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
    @ApiResponses({
            @ApiResponse(code = NOT_FOUND_CODE, message = "Library was never scanned.", response = ErrorDto.class),
    })
    public ScanStatisticsDto getStatistics() throws ObjectNotFoundException {
        return scanFacade.getScanStatistics();
    }
}
