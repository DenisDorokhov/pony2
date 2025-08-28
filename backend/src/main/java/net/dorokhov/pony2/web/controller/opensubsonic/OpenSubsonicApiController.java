package net.dorokhov.pony2.web.controller.opensubsonic;

import net.dorokhov.pony2.web.dto.*;
import net.dorokhov.pony2.web.dto.opensubsonic.*;
import net.dorokhov.pony2.web.dto.opensubsonic.response.*;
import net.dorokhov.pony2.web.service.*;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    private final PlaybackHistoryFacade playbackHistoryFacade;

    public OpenSubsonicApiController(
            OpenSubsonicResponseService openSubsonicResponseService,
            UserFacade userFacade,
            LibraryFacade libraryFacade,
            PlaylistFacade playlistFacade,
            PlaybackHistoryFacade playbackHistoryFacade
    ) {
        this.openSubsonicResponseService = openSubsonicResponseService;
        this.userFacade = userFacade;
        this.libraryFacade = libraryFacade;
        this.playlistFacade = playlistFacade;
        this.playbackHistoryFacade = playbackHistoryFacade;
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

    private String formatDate(LocalDateTime date) {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(date, ZoneId.systemDefault()).withZoneSameInstant(ZoneId.systemDefault());
        return zonedDateTime.format(DateTimeFormatter.ISO_DATE);
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

    @RequestMapping(value = "/opensubsonic/rest/getStarred2.view", method = {GET, POST})
    public OpenSubsonicResponseDto<?> getStarred2() {
        PlaylistSongsDto likePlaylist = playlistFacade.getLikePlaylist();
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicStarred2ResponseDto()
                .setStarred2(new OpenSubsonicStarred2()
                        .setArtist(List.of())
                        .setAlbum(List.of())
                        .setSong(likePlaylist.getSongs().stream()
                                .map(song -> toChild(song, likePlaylist))
                                .toList())
                ));
    }

    private OpenSubsonicChild toChild(SongDetailsDto songDetails, PlaylistSongsDto likePlaylist) {
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
                .setDiscNumber(songDetails.getSong().getDiscNumber() != null ? songDetails.getSong().getDiscNumber() : 1)
                .setCreated(formatDate(songDetails.getSong().getCreationDate()))
                .setStarred(likePlaylist.getSongs().stream()
                        .filter(next -> next.getId().equals(songDetails.getSong().getId()))
                        .map(PlaylistSongDto::getCreationDate)
                        .map(this::formatDate)
                        .findAny()
                        .orElse(null))
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

    private OpenSubsonicChild toChild(PlaylistSongDto song, PlaylistSongsDto likePlaylist) {
        return toChild(song.getSong(), likePlaylist);
    }

    @RequestMapping(value = "/opensubsonic/rest/getBookmarks.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicBookmarksResponseDto> getBookmarks() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicBookmarksResponseDto()
                .setBookmarks(new OpenSubsonicBookmarksResponseDto.Bookmarks()
                        .setBookmark(List.of())));
    }

    @RequestMapping(value = "/opensubsonic/rest/getAlbumList2.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicAlbumList2ResponseDto> getAlbumList2(
            @RequestParam int size, @RequestParam int offset
    ) {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicAlbumList2ResponseDto()
                .setAlbumList2(
                        new OpenSubsonicAlbumList2ResponseDto.AlbumList2().setAlbum(
                                libraryFacade.getAlbums(size, offset).stream()
                                        .map(this::toAlbumID3)
                                        .toList()
                        )
                ));
    }

    private OpenSubsonicAlbumID3 toAlbumID3(AlbumSongDetailsDto albumSongs) {
        String genre = resolveGenre(albumSongs);
        return new OpenSubsonicAlbumID3()
                .setId(albumSongs.getDetails().getAlbum().getId())
                .setName(albumSongs.getDetails().getAlbum().getName())
                .setArtist(albumSongs.getDetails().getArtist().getName())
                .setArtistId(albumSongs.getDetails().getArtist().getId())
                .setCoverArt(albumSongs.getDetails().getAlbum().getArtworkId())
                .setCreated(formatDate(albumSongs.getDetails().getAlbum().getCreationDate()))
                .setYear(albumSongs.getDetails().getAlbum().getYear())
                .setGenre(genre)
                .setGenres(genre != null ? List.of(new OpenSubsonicItemGenre().setName(genre)) : null)
                .setArtists(List.of(
                        toArtistID3(albumSongs.getDetails().getArtist())
                ))
                .setDisplayArtist(albumSongs.getDetails().getArtist().getName())
                ;
    }

    private String resolveGenre(AlbumSongDetailsDto album) {
        return album.getSongs().stream()
                .filter(song -> song.getGenre().getName() != null)
                .map(song -> song.getGenre().getName())
                .findAny()
                .orElse(null);
    }

    @RequestMapping(value = "/opensubsonic/rest/getAlbum.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicAlbumResponseDto> getAlbum(@RequestParam String id) throws ObjectNotFoundException {
        AlbumSongDetailsDto albumSongs = libraryFacade.getAlbumSongDetails(id);
        PlaylistSongsDto likePlaylist = playlistFacade.getLikePlaylist();
        String genre = resolveGenre(albumSongs);
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicAlbumResponseDto()
                .setAlbum(new OpenSubsonicAlbumID3WithSongs()
                        .setId(albumSongs.getDetails().getAlbum().getId())
                        .setName(albumSongs.getDetails().getAlbum().getName())
                        .setArtist(albumSongs.getDetails().getArtist().getName())
                        .setArtistId(albumSongs.getDetails().getArtist().getId())
                        .setCoverArt(albumSongs.getDetails().getAlbum().getArtworkId())
                        .setCreated(formatDate(albumSongs.getDetails().getAlbum().getCreationDate()))
                        .setYear(albumSongs.getDetails().getAlbum().getYear())
                        .setArtists(List.of(
                                toArtistID3(albumSongs.getDetails().getArtist())
                        ))
                        .setDisplayArtist(albumSongs.getDetails().getArtist().getName())
                        .setGenre(genre)
                        .setGenres(genre != null ? List.of(new OpenSubsonicItemGenre().setName(genre)) : null)
                        .setSong(albumSongs.getSongs().stream()
                                .map(song -> toChild(song, likePlaylist))
                                .toList())
                )
        );
    }

    @RequestMapping(value = "/opensubsonic/rest/getAlbumInfo2.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicAlbumInfo2ResponseDto> getAlbumInfo2() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicAlbumInfo2ResponseDto()
                .setAlbumInfo(new OpenSubsonicAlbumInfo()));
    }

    @RequestMapping(value = "/opensubsonic/rest/getArtistInfo2.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicArtistInfo2ResponseDto> getArtistInfo2() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicArtistInfo2ResponseDto()
                .setArtistInfo2(new OpenSubsonicArtistInfo2()));
    }

    @RequestMapping(value = "/opensubsonic/rest/scrobble.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicEmptyResponseDto> scrobble(@RequestParam String id, @RequestParam(defaultValue = "true") boolean submission) throws ObjectNotFoundException {
        if (submission) {
            playbackHistoryFacade.addSongToHistory(id);
        }
        return openSubsonicResponseService.createSuccessful();
    }

    @RequestMapping(value = "/opensubsonic/rest/star.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicEmptyResponseDto> star(@RequestParam(required = false) String id) throws ObjectNotFoundException {
        if (id != null) {
            playlistFacade.likeSong(id);
        }
        return openSubsonicResponseService.createSuccessful();
    }

    @RequestMapping(value = "/opensubsonic/rest/unstar.view", method = {GET, POST})
    public OpenSubsonicResponseDto<OpenSubsonicEmptyResponseDto> unstar(@RequestParam(required = false) String id) throws ObjectNotFoundException {
        if (id != null) {
            playlistFacade.unlikeSong(id);
        }
        return openSubsonicResponseService.createSuccessful();
    }
}
