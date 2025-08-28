package net.dorokhov.pony2.web.controller.opensubsonic;

import net.dorokhov.pony2.web.dto.ArtistDto;
import net.dorokhov.pony2.web.dto.ArtistSongsDto;
import net.dorokhov.pony2.web.dto.PlaylistSongDto;
import net.dorokhov.pony2.web.dto.SongDetailsDto;
import net.dorokhov.pony2.web.dto.opensubsonic.*;
import net.dorokhov.pony2.web.dto.opensubsonic.response.*;
import net.dorokhov.pony2.web.service.LibraryFacade;
import net.dorokhov.pony2.web.service.OpenSubsonicResponseService;
import net.dorokhov.pony2.web.service.PlaylistFacade;
import net.dorokhov.pony2.web.service.UserFacade;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class OpenSubsonicApiController implements OpenSubsonicController {

    private final OpenSubsonicResponseService openSubsonicResponseService;
    private final UserFacade userFacade;
    private final LibraryFacade libraryFacade;
    private final PlaylistFacade playlistFacade;

    public OpenSubsonicApiController(
            OpenSubsonicResponseService openSubsonicResponseService,
            UserFacade userFacade,
            LibraryFacade libraryFacade,
            PlaylistFacade playlistFacade
    ) {
        this.openSubsonicResponseService = openSubsonicResponseService;
        this.userFacade = userFacade;
        this.libraryFacade = libraryFacade;
        this.playlistFacade = playlistFacade;
    }

    @RequestMapping(value = "/opensubsonic/rest/ping.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicEmptyResponseDto> ping() {
        return openSubsonicResponseService.createSuccessful();
    }

    @RequestMapping(value = "/opensubsonic/rest/getLicense.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicLicenseResponseDto> getLicense() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicLicenseResponseDto(new OpenSubsonicLicense()
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
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicTokenInfoResponseDto(new OpenSubsonicTokenInfo()
                .setUsername(userFacade.getCurrentUser().getName())));
    }

    @RequestMapping(value = "/opensubsonic/rest/getMusicFolders.view", method = {GET, POST})
    public OpenSubsonicResponseDto<?> getMusicFolders() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicMusicFoldersResponseDto()
                .setMusicFolders(new OpenSubsonicMusicFoldersResponseDto.MusicFolders().setMusicFolder(List.of(
                        new OpenSubsonicMusicFolder()
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
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicGenresResponseDto()
                .setGenres(new OpenSubsonicGenresResponseDto.Genres()
                        .setGenre(libraryFacade.getGenres().stream()
                                .map(genre -> new OpenSubsonicGenre()
                                        .setValue(genre.getName())
                                )
                                .toList()
                        )));
    }

    @RequestMapping(value = "/opensubsonic/rest/getArtists.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicArtistsResponseDto> getArtists() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicArtistsResponseDto().setArtists(toArtistsID3(libraryFacade.getArtists().stream()
                .map(OpenSubsonicApiController::toArtistID3)
                .toList()
        )));
    }

    private static OpenSubsonicArtistID3 toArtistID3(ArtistDto artist) {
        return new OpenSubsonicArtistID3()
                .setId(artist.getId())
                .setName(artist.getName() != null ? artist.getName() : "Unknown")
                .setCoverArt(artist.getArtworkId());
    }

    private OpenSubsonicArtistsID3 toArtistsID3(List<OpenSubsonicArtistID3> artists) {
        Map<String, List<OpenSubsonicArtistID3>> letterToArtists = new TreeMap<>();
        for (OpenSubsonicArtistID3 artist : artists) {
            String letter = artist.getName() != null ? artist.getName().substring(0, 1).toUpperCase() : "U";
            List<OpenSubsonicArtistID3> letterArtists = letterToArtists.computeIfAbsent(letter, k -> new ArrayList<>());
            letterArtists.add(artist);
        }
        return new OpenSubsonicArtistsID3()
                .setIgnoredArticles("")
                .setIndex(letterToArtists.entrySet().stream().map(entry -> new OpenSubsonicIndexID3()
                                .setName(entry.getKey())
                                .setArtist(entry.getValue()))
                        .toList());
    }

    @RequestMapping(value = "/opensubsonic/rest/getArtist.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicArtistResponseDto> getArtists(@RequestParam String id) throws ObjectNotFoundException {
        ArtistSongsDto artistSongs = libraryFacade.getArtistSongs(id);
        OpenSubsonicArtistResponseDto response = new OpenSubsonicArtistResponseDto()
                .setArtist(new OpenSubsonicArtistWithAlbumsID3()
                        .setId(artistSongs.getArtist().getId())
                        .setName(artistSongs.getArtist().getName() != null ? artistSongs.getArtist().getName() : "Unknown")
                        .setCoverArt(artistSongs.getArtist().getArtworkId())
                        .setAlbum(artistSongs.getAlbumSongs().stream()
                                .map(album -> new OpenSubsonicArtistWithAlbumsID3.Album()
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

    @RequestMapping(value = "/opensubsonic/rest/getStarred2.view", method = {GET, POST})
    public OpenSubsonicResponseDto<?> getStarred2() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicStarred2ResponseDto()
                .setStarred2(new OpenSubsonicStarred2()
                        .setArtist(List.of())
                        .setAlbum(List.of())
                        .setSong(playlistFacade.getLikePlaylist().getSongs().stream()
                                .map(this::toChild)
                                .toList())
                ));
    }

    private OpenSubsonicChild toChild(SongDetailsDto songDetails) {
        return new OpenSubsonicChild()
                .setId(songDetails.getSong().getId())
                .setParent(songDetails.getAlbumDetails().getAlbum().getId())
                .setDir(false)
                .setTitle(songDetails.getSong().getName())
                .setAlbum(songDetails.getAlbumDetails().getAlbum().getName())
                .setArtist(songDetails.getAlbumDetails().getArtist().getName())
                .setTrack(songDetails.getSong().getTrackNumber())
                .setYear(songDetails.getAlbumDetails().getAlbum().getYear())
                .setGenre(songDetails.getGenre().getName())
                .setCoverArt(songDetails.getAlbumDetails().getAlbum().getArtworkId())
                .setSize(songDetails.getSong().getSize())
                .setContentType(songDetails.getSong().getMimeType())
                .setSuffix("mp3")
                .setTranscodedContentType(songDetails.getSong().getMimeType())
                .setTranscodedSuffix("mp3")
                .setDuration(songDetails.getSong().getDuration().intValue())
                .setBitRate(songDetails.getSong().getBitRate().intValue())
                .setPath(songDetails.getSong().getPath())
                .setVideo(false)
                .setDiscNumber(songDetails.getSong().getDiscNumber())
                .setCreated(formatDate(songDetails.getSong().getCreationDate()))
                .setAlbumId(songDetails.getAlbumDetails().getAlbum().getId())
                .setArtistId(songDetails.getAlbumDetails().getArtist().getId())
                .setType("music")
                .setMediaType("song")
                .setGenres(songDetails.getSong().getGenreName() != null ? List.of(
                        new OpenSubsonicItemGenre().setName(songDetails.getSong().getGenreName())
                ) : List.of())
                .setArtists(List.of(
                        toArtistID3(songDetails.getAlbumDetails().getArtist())
                ))
                .setDisplayArtist(songDetails.getSong().getArtistName())
                .setAlbumArtists(List.of(
                        toArtistID3(songDetails.getAlbumDetails().getArtist())
                ))
                .setDisplayAlbumArtist(songDetails.getAlbumDetails().getArtist().getName())
                ;
    }

    private OpenSubsonicChild toChild(PlaylistSongDto song) {
        return toChild(song.getSong());
    }

    @RequestMapping(value = "/opensubsonic/rest/getBookmarks.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicBookmarksResponseDto> getBookmarks() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicBookmarksResponseDto()
                .setBookmarks(new OpenSubsonicBookmarksResponseDto.Bookmarks()
                        .setBookmark(List.of())));
    }

    @RequestMapping(value = "/opensubsonic/rest/getAlbumList2.view", method = {GET, POST})
    public OpenSubsonicResponseDto<?> getAlbumList2() {
        throw new UnsupportedOperationException();
    }
}
