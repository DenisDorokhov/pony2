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

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static net.dorokhov.pony.web.controller.common.SwaggerResponses.UNAUTHORIZED_MESSAGE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/library", produces = APPLICATION_JSON_VALUE)
@Api(tags = "Library")
@ApiResponses({
        @ApiResponse(code = SC_UNAUTHORIZED, message = UNAUTHORIZED_MESSAGE, response = ErrorDto.class),
})
public class LibraryController implements ErrorHandlingController {

    private final static int MAX_RANDOM_COUNT = 30;

    private final LibraryFacade libraryFacade;
    private final ScanFacade scanFacade;

    public LibraryController(LibraryFacade libraryFacade, ScanFacade scanFacade) {
        this.libraryFacade = libraryFacade;
        this.scanFacade = scanFacade;
    }
    
    @GetMapping("/song/{songIds}")
    @ApiOperation("Get songs by IDs.")
    public List<SongDetailsDto> getSong(@PathVariable List<String> songIds) {
        return libraryFacade.getSongs(songIds);
    }

    @GetMapping("/artists")
    @ApiOperation("Get list of artists.")
    public List<ArtistDto> getArtists() {
        return libraryFacade.getArtists();
    }

    @GetMapping("/artistSongs/{artistId}")
    @ApiOperation("Get artist songs by artist ID.")
    @ApiResponses({
            @ApiResponse(code = SC_NOT_FOUND, message = "Requested artist not found.", response = ErrorDto.class),
    })
    public ArtistSongsDto getArtistSongs(@PathVariable String artistId) throws ObjectNotFoundException {
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
            @ApiResponse(code = SC_NOT_FOUND, message = "Requested genre not found.", response = ErrorDto.class),
    })
    public GenreSongsPageDto getGenreSongs(@PathVariable String genreId, @RequestParam(defaultValue = "0") int pageIndex) throws ObjectNotFoundException {
        return libraryFacade.getGenreSongs(genreId, pageIndex);
    }

    @GetMapping("/search")
    @ApiOperation("Search genres, artists, albums and songs.")
    public SearchResultDto search(@RequestParam String query) {
        return libraryFacade.search(query);
    }

    @GetMapping("/randomSongs")
    @ApiOperation("Get random songs.")
    public List<SongDetailsDto> getRandomSongs(@RequestParam(defaultValue = "10") int count) {
        return libraryFacade.getRandomSongs(Math.min(count, MAX_RANDOM_COUNT));
    }

    @GetMapping("/randomArtistSongs/{artistId}")
    @ApiOperation("Get random songs of an artist provided.")
    public List<SongDetailsDto> getArtistRandomSongs(@PathVariable String artistId, @RequestParam(defaultValue = "10") int count) {
        return libraryFacade.getRandomSongsByArtistId(artistId, Math.min(count, MAX_RANDOM_COUNT));
    }

    @GetMapping("/randomAlbumSongs/{albumId}")
    @ApiOperation("Get random songs of an album provided.")
    public List<SongDetailsDto> getAlbumRandomSongs(@PathVariable String albumId, @RequestParam(defaultValue = "10") int count) {
        return libraryFacade.getRandomSongsByAlbumId(albumId, Math.min(count, MAX_RANDOM_COUNT));
    }

    @GetMapping("/randomGenreSongs/{genreId}")
    @ApiOperation("Get random songs of an genre provided.")
    public List<SongDetailsDto> getGenreRandomSongs(@PathVariable String genreId, @RequestParam(defaultValue = "10") int count) {
        return libraryFacade.getRandomSongsByGenreId(genreId, Math.min(count, MAX_RANDOM_COUNT));
    }

    @GetMapping("/scanStatus")
    @ApiOperation("Get status for currently running scan.")
    public ScanStatusDto getScanStatus() {
        return scanFacade.getScanStatus();
    }

    @GetMapping("/scanStatistics")
    @ApiOperation("Get last scan statistics.")
    @ApiResponses({
            @ApiResponse(code = SC_NOT_FOUND, message = "Library was never scanned.", response = ErrorDto.class),
    })
    public ScanStatisticsDto getStatistics() throws ObjectNotFoundException {
        return scanFacade.getScanStatistics();
    }
}
