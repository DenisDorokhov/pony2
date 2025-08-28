package net.dorokhov.pony2.web.controller.opensubsonic;

import net.dorokhov.pony2.web.dto.ArtistSongsDto;
import net.dorokhov.pony2.web.dto.opensubsonic.*;
import net.dorokhov.pony2.web.service.LibraryFacade;
import net.dorokhov.pony2.web.service.OpenSubsonicResponseService;
import net.dorokhov.pony2.web.service.UserFacade;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class OpenSubsonicApiController implements OpenSubsonicController {

    private final OpenSubsonicResponseService openSubsonicResponseService;
    private final UserFacade userFacade;
    private final LibraryFacade libraryFacade;

    public OpenSubsonicApiController(
            OpenSubsonicResponseService openSubsonicResponseService,
            UserFacade userFacade,
            LibraryFacade libraryFacade
    ) {
        this.openSubsonicResponseService = openSubsonicResponseService;
        this.userFacade = userFacade;
        this.libraryFacade = libraryFacade;
    }

    @RequestMapping(value = "/opensubsonic/rest/ping.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicEmptyResponseDto> ping() {
        return openSubsonicResponseService.createSuccessful();
    }

    @RequestMapping(value = "/opensubsonic/rest/getLicense.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicLicenseResponseDto> getLicense() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicLicenseResponseDto(new OpenSubsonicLicenseResponseDto.License()
                .setValid(true)
                .setEmail(userFacade.getCurrentUser().getEmail())
                .setLicenseExpires(formatDate(LocalDateTime.now().plusYears(1)))
                .setTrialExpires("2025-01-01T00:00:00.000Z")
        ));
    }

    @RequestMapping(value = "/opensubsonic/rest/getOpenSubsonicExtensions.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicExtensionsResponseDto> getOpenSubsonicExtensions() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicExtensionsResponseDto());
    }

    @RequestMapping(value = "/opensubsonic/rest/tokenInfo.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicTokenInfoResponseDto> tokenInfo() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicTokenInfoResponseDto(new OpenSubsonicTokenInfoResponseDto.TokenInfo()
                .setUsername(userFacade.getCurrentUser().getName())));
    }

    @RequestMapping(value = "/opensubsonic/rest/getMusicFolders.view", method = {GET, POST})
    public OpenSubsonicResponseDto<?> getMusicFolders() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicMusicFoldersResponseDto()
                .setMusicFolders(new OpenSubsonicMusicFoldersResponseDto.MusicFolders().setMusicFolder(List.of(
                        new OpenSubsonicMusicFoldersResponseDto.MusicFolders.MusicFolder()
                                .setId(1)
                                .setName("Music")
                ))));
    }

    @RequestMapping(value = "/opensubsonic/rest/getIndexes.view", method = {GET, POST})
    public OpenSubsonicResponseDto<?> getIndexes() {
        throw new UnsupportedOperationException();
    }

    @RequestMapping(value = "/opensubsonic/rest/getMusicDirectory.view", method = {GET, POST})
    public OpenSubsonicResponseDto<?> getMusicDirectory() {
        throw new UnsupportedOperationException();
    }

    @RequestMapping(value = "/opensubsonic/rest/getGenres.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicGenresResponseDto> getGenres() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicGenresResponseDto(libraryFacade.getGenres().stream()
                .map(genre -> new OpenSubsonicGenresResponseDto.Genres.Genre()
                        .setValue(genre.getName())
                )
                .toList()
        ));
    }

    @RequestMapping(value = "/opensubsonic/rest/getArtists.view", method = {GET, POST})
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

    @RequestMapping(value = "/opensubsonic/rest/getArtist.view", method = {GET, POST})
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
}
