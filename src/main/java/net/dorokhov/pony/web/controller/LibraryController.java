package net.dorokhov.pony.web.controller;

import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;
import net.dorokhov.pony.web.domain.*;
import net.dorokhov.pony.web.service.LibraryFacade;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/library")
public class LibraryController implements ErrorHandlingController {
    
    private final LibraryFacade libraryFacade;

    public LibraryController(LibraryFacade libraryFacade) {
        this.libraryFacade = libraryFacade;
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
    public GenreSongsDto getGenreSongs(@PathVariable Long genreId,
                                       @RequestParam(defaultValue = "0") int pageIndex) throws ObjectNotFoundException {
        return libraryFacade.getGenreSongs(genreId, pageIndex);
    }
    
    @GetMapping("/search")
    public SearchResultDto search(@RequestParam String query) {
        return libraryFacade.search(query);
    }
    
    @GetMapping("/scanStatus")
    public ScanStatusDto getScanStatus() {
        return libraryFacade.getScanStatus();
    }
}
