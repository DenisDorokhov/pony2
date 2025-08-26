package net.dorokhov.pony2.web.controller.opensubsonic;

import net.dorokhov.pony2.web.dto.ArtistSongsDto;
import net.dorokhov.pony2.web.dto.opensubsonic.*;
import net.dorokhov.pony2.web.service.LibraryFacade;
import net.dorokhov.pony2.web.service.OpenSubsonicResponseService;
import net.dorokhov.pony2.web.service.UserFacade;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static net.dorokhov.pony2.web.service.OpenSubsonicResponseService.ERROR_GENERIC;
import static net.dorokhov.pony2.web.service.OpenSubsonicResponseService.ERROR_NOT_FOUND;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class OpenSubsonicController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final OpenSubsonicResponseService openSubsonicResponseService;
    private final UserFacade userFacade;
    private final LibraryFacade libraryFacade;

    public OpenSubsonicController(
            OpenSubsonicResponseService openSubsonicResponseService,
            UserFacade userFacade,
            LibraryFacade libraryFacade
    ) {
        this.openSubsonicResponseService = openSubsonicResponseService;
        this.userFacade = userFacade;
        this.libraryFacade = libraryFacade;
    }

    @RequestMapping(value = "/rest/ping.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicEmptyResponseDto> ping() {
        return openSubsonicResponseService.createSuccessful();
    }

    @RequestMapping(value = "/rest/getLicense.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicLicenseResponseDto> getLicense() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicLicenseResponseDto(new OpenSubsonicLicenseResponseDto.License()
                .setValid(true)
                .setEmail(userFacade.getCurrentUser().getEmail())
                .setLicenseExpires(formatDate(LocalDateTime.now().plusYears(1)))
                .setTrialExpires("2025-01-01T00:00:00.000Z")
        ));
    }

    @RequestMapping(value = "/rest/getOpenSubsonicExtensions.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicExtensionsResponseDto> getOpenSubsonicExtensions() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicExtensionsResponseDto());
    }

    @RequestMapping(value = "/rest/tokenInfo.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicTokenInfoResponseDto> tokenInfo() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicTokenInfoResponseDto(new OpenSubsonicTokenInfoResponseDto.TokenInfo()
                .setUsername(userFacade.getCurrentUser().getName())));
    }

    @RequestMapping(value = "/rest/getMusicFolders.view", method = {GET, POST})
    public OpenSubsonicResponseDto<?> getMusicFolders() {
        throw new UnsupportedOperationException();
    }

    @RequestMapping(value = "/rest/getIndexes.view", method = {GET, POST})
    public OpenSubsonicResponseDto<?> getIndexes() {
        throw new UnsupportedOperationException();
    }

    @RequestMapping(value = "/rest/getMusicDirectory.view", method = {GET, POST})
    public OpenSubsonicResponseDto<?> getMusicDirectory() {
        throw new UnsupportedOperationException();
    }

    @RequestMapping(value = "/rest/getGenres.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicGenresResponseDto> getGenres() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicGenresResponseDto(libraryFacade.getGenres().stream()
                .map(genre -> new OpenSubsonicGenresResponseDto.Genres.Genre()
                        .setValue(genre.getName())
                )
                .toList()
        ));
    }

    @RequestMapping(value = "/rest/getArtists.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicArtistsResponseDto> getArtists() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicArtistsResponseDto(libraryFacade.getArtists().stream()
                .map(artist -> new OpenSubsonicArtistsResponseDto.Artists.Index.Artist()
                        .setId(artist.getId())
                        .setName(artist.getName() != null ? artist.getName() : "Unknown")
                        .setCoverArt(artist.getArtworkId())
                )
                .toList()
        ));
    }

    @RequestMapping(value = "/rest/getArtist.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicArtistResponseDto> getArtists(@RequestParam String id) throws ObjectNotFoundException {
        ArtistSongsDto artistSongs = libraryFacade.getArtistSongs(id);
        OpenSubsonicArtistResponseDto response = new OpenSubsonicArtistResponseDto()
                .setArtist(new OpenSubsonicArtistResponseDto.Artist()
                        .setId(artistSongs.getArtist().getId())
                        .setName(artistSongs.getArtist().getName() != null ? artistSongs.getArtist().getName() : "Unknown")
                        .setCoverArt(artistSongs.getArtist().getArtworkId())
                        .setAlbum(artistSongs.getAlbumSongs().stream()
                                .map(album -> new OpenSubsonicArtistResponseDto.Artist.Album()
                                        .setId(album.getAlbum().getId())
                                        .setName(album.getAlbum().getName() != null ? album.getAlbum().getName() : "Unknown")
                                        .setArtist(artistSongs.getArtist().getName() != null ? artistSongs.getArtist().getName() : "Unknown")
                                        .setArtistId(artistSongs.getArtist().getId())
                                        .setCoverArt(album.getAlbum().getArtworkId())
                                        .setCreated(formatDate(album.getAlbum().getCreationDate()))
                                        .setYear(album.getAlbum().getYear())
                                )
                                .toList())
                );
        return openSubsonicResponseService.createSuccessful(response);
    }

    private String formatDate(LocalDateTime date) {
        return date.format(DateTimeFormatter.ISO_INSTANT);
    }

    @ControllerAdvice(assignableTypes = OpenSubsonicController.class)
    @ResponseBody
    class Advice {

        private final OpenSubsonicResponseService responseFactory;

        public Advice(OpenSubsonicResponseService responseFactory) {
            this.responseFactory = responseFactory;
        }

        @ExceptionHandler(ObjectNotFoundException.class)
        @ResponseStatus(HttpStatus.OK)
        public Object onObjectNotFound(ObjectNotFoundException e) {
            logger.info("Object not found.", e);
            return responseFactory.createError(ERROR_NOT_FOUND, "Object not found.");
        }

        @ExceptionHandler(Exception.class)
        @ResponseStatus(HttpStatus.OK)
        public Object onUnexpectedError(Exception e) {
            logger.error("Unexpected error occurred.", e);
            return responseFactory.createError(ERROR_GENERIC, "Unexpected error occurred.");
        }
    }
}
