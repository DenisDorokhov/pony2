package net.dorokhov.pony2.web.controller.opensubsonic;

import net.dorokhov.pony2.api.library.domain.Playlist;
import net.dorokhov.pony2.web.dto.*;
import net.dorokhov.pony2.web.dto.opensubsonic.*;
import net.dorokhov.pony2.web.dto.opensubsonic.response.*;
import net.dorokhov.pony2.web.service.LibraryFacade;
import net.dorokhov.pony2.web.service.OpenSubsonicResponseService;
import net.dorokhov.pony2.web.service.PlaybackHistoryFacade;
import net.dorokhov.pony2.web.service.PlaylistFacade;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class OpenSubsonicApiController implements OpenSubsonicController {

    private final OpenSubsonicResponseService openSubsonicResponseService;
    private final LibraryFacade libraryFacade;
    private final PlaylistFacade playlistFacade;
    private final PlaybackHistoryFacade playbackHistoryFacade;

    public OpenSubsonicApiController(
            OpenSubsonicResponseService openSubsonicResponseService,
            LibraryFacade libraryFacade,
            PlaylistFacade playlistFacade,
            PlaybackHistoryFacade playbackHistoryFacade
    ) {
        this.openSubsonicResponseService = openSubsonicResponseService;
        this.libraryFacade = libraryFacade;
        this.playlistFacade = playlistFacade;
        this.playbackHistoryFacade = playbackHistoryFacade;
    }

    @RequestMapping(value = "/opensubsonic/rest/ping.view", method = {GET, POST})
    public OpenSubsonicResponse<OpenSubsonicEmptyResponse> ping() {
        return openSubsonicResponseService.createSuccessful();
    }

    @RequestMapping(value = "/opensubsonic/rest/getOpenSubsonicExtensions.view", method = {GET, POST})
    public OpenSubsonicResponse<OpenSubsonicExtensionsResponse> getOpenSubsonicExtensions() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicExtensionsResponse()
                .setOpenSubsonicExtensions(List.of(
                        new OpenSubsonicExtension()
                                .setName("apiKeyAuthentication")
                                .setVersions(List.of(1)),
                        new OpenSubsonicExtension()
                                .setName("formPost")
                                .setVersions(List.of(1))
                )));
    }

    @RequestMapping(value = "/opensubsonic/rest/getMusicFolders.view", method = {GET, POST})
    public OpenSubsonicResponse<OpenSubsonicMusicFoldersResponse> getMusicFolders() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicMusicFoldersResponse()
                .setMusicFolders(new OpenSubsonicMusicFoldersResponse.MusicFolders().setMusicFolder(List.of(
                        new OpenSubsonicMusicFolder()
                                .setId(1)
                                .setName("Pony")
                ))));
    }

    @RequestMapping(value = "/opensubsonic/rest/getGenres.view", method = {GET, POST})
    public OpenSubsonicResponse<OpenSubsonicGenresResponse> getGenres() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicGenresResponse()
                .setGenres(new OpenSubsonicGenresResponse.Genres()
                        .setGenre(libraryFacade.getGenres().stream()
                                .map(genre -> new OpenSubsonicGenre()
                                        .setValue(nullToUnknown(genre.getName()))
                                )
                                .toList()
                        )));
    }

    private String nullToUnknown(String value) {
        return value != null ? value : "Unknown";
    }

    @RequestMapping(value = "/opensubsonic/rest/getArtists.view", method = {GET, POST})
    public OpenSubsonicResponse<OpenSubsonicArtistsResponse> getArtists() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicArtistsResponse()
                .setArtists(toArtistsID3(libraryFacade.getArtists().stream()
                        .map(this::toArtistID3)
                        .toList()
                )));
    }

    private OpenSubsonicArtistID3 toArtistID3(ArtistDto artist) {
        return new OpenSubsonicArtistID3()
                .setId(artist.getId())
                .setName(nullToUnknown(artist.getName()))
                .setCoverArt(artist.getArtworkId());
    }

    private OpenSubsonicArtistsID3 toArtistsID3(List<OpenSubsonicArtistID3> artists) {
        Map<String, List<OpenSubsonicArtistID3>> letterToArtists = new TreeMap<>();
        for (OpenSubsonicArtistID3 artist : artists) {
            String letter = nullToUnknown(artist.getName()).substring(0, 1).toUpperCase();
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
    public OpenSubsonicResponse<OpenSubsonicStarred2Response> getStarred2() {
        PlaylistSongsDto likePlaylist = playlistFacade.getLikePlaylist();
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicStarred2Response()
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
                .setTitle(nullToUnknown(songDetails.getSong().getName()))
                .setAlbum(nullToUnknown(songDetails.getAlbumDetails().getAlbum().getName()))
                .setArtist(nullToUnknown(songDetails.getAlbumDetails().getArtist().getName()))
                .setTrack(songDetails.getSong().getTrackNumber())
                .setYear(songDetails.getAlbumDetails().getAlbum().getYear())
                .setGenre(nullToUnknown(songDetails.getGenre().getName()))
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
                        .filter(next -> next.getSong().getSong().getId().equals(songDetails.getSong().getId()))
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
                .setDisplayAlbumArtist(nullToUnknown(songDetails.getAlbumDetails().getArtist().getName()))
                ;
    }

    private String formatDate(@Nullable LocalDateTime date) {
        if (date == null) {
            return null;
        }
        ZonedDateTime zonedDateTime = ZonedDateTime.of(date, ZoneId.systemDefault()).withZoneSameInstant(ZoneId.systemDefault());
        return zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private OpenSubsonicChild toChild(PlaylistSongDto song, PlaylistSongsDto likePlaylist) {
        return toChild(song.getSong(), likePlaylist);
    }

    @RequestMapping(value = "/opensubsonic/rest/deleteBookmark.view", method = {GET, POST})
    public OpenSubsonicResponse<OpenSubsonicEmptyResponse> deleteBookmark() {
        return openSubsonicResponseService.createSuccessful();
    }

    @RequestMapping(value = "/opensubsonic/rest/createBookmark.view", method = {GET, POST})
    public OpenSubsonicResponse<OpenSubsonicEmptyResponse> createBookmark() {
        return openSubsonicResponseService.createSuccessful();
    }

    @RequestMapping(value = "/opensubsonic/rest/getBookmarks.view", method = {GET, POST})
    public OpenSubsonicResponse<OpenSubsonicBookmarksResponse> getBookmarks() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicBookmarksResponse()
                .setBookmarks(new OpenSubsonicBookmarksResponse.Bookmarks()
                        .setBookmark(List.of())));
    }

    @RequestMapping(value = "/opensubsonic/rest/getAlbumList2.view", method = {GET, POST})
    public OpenSubsonicResponse<OpenSubsonicAlbumList2Response> getAlbumList2(
            @RequestParam int size, @RequestParam int offset
    ) {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicAlbumList2Response()
                .setAlbumList2(
                        new OpenSubsonicAlbumList2Response.AlbumList2().setAlbum(
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
                .setName(nullToUnknown(albumSongs.getDetails().getAlbum().getName()))
                .setArtist(nullToUnknown(albumSongs.getDetails().getArtist().getName()))
                .setArtistId(albumSongs.getDetails().getArtist().getId())
                .setCoverArt(albumSongs.getDetails().getAlbum().getArtworkId())
                .setCreated(formatDate(albumSongs.getDetails().getAlbum().getCreationDate()))
                .setYear(albumSongs.getDetails().getAlbum().getYear())
                .setGenre(genre)
                .setGenres(genre != null ? List.of(new OpenSubsonicItemGenre().setName(genre)) : null)
                .setArtists(List.of(
                        toArtistID3(albumSongs.getDetails().getArtist())
                ))
                .setDisplayArtist(nullToUnknown(albumSongs.getDetails().getArtist().getName()))
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
    public OpenSubsonicResponse<OpenSubsonicAlbumResponse> getAlbum(@RequestParam String id) throws ObjectNotFoundException {
        AlbumSongDetailsDto albumSongs = libraryFacade.getAlbumSongDetails(id);
        PlaylistSongsDto likePlaylist = playlistFacade.getLikePlaylist();
        String genre = resolveGenre(albumSongs);
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicAlbumResponse()
                .setAlbum(new OpenSubsonicAlbumID3WithSongs()
                        .setId(albumSongs.getDetails().getAlbum().getId())
                        .setName(nullToUnknown(albumSongs.getDetails().getAlbum().getName()))
                        .setArtist(nullToUnknown(albumSongs.getDetails().getArtist().getName()))
                        .setArtistId(albumSongs.getDetails().getArtist().getId())
                        .setCoverArt(albumSongs.getDetails().getAlbum().getArtworkId())
                        .setCreated(formatDate(albumSongs.getDetails().getAlbum().getCreationDate()))
                        .setYear(albumSongs.getDetails().getAlbum().getYear())
                        .setArtists(List.of(
                                toArtistID3(albumSongs.getDetails().getArtist())
                        ))
                        .setDisplayArtist(nullToUnknown(albumSongs.getDetails().getArtist().getName()))
                        .setGenre(genre)
                        .setGenres(genre != null ? List.of(new OpenSubsonicItemGenre().setName(genre)) : null)
                        .setSong(albumSongs.getSongs().stream()
                                .map(song -> toChild(song, likePlaylist))
                                .toList())
                )
        );
    }

    @RequestMapping(value = "/opensubsonic/rest/getAlbumInfo2.view", method = {GET, POST})
    public OpenSubsonicResponse<OpenSubsonicAlbumInfo2Response> getAlbumInfo2() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicAlbumInfo2Response()
                .setAlbumInfo(new OpenSubsonicAlbumInfo()));
    }

    @RequestMapping(value = "/opensubsonic/rest/getArtistInfo2.view", method = {GET, POST})
    public OpenSubsonicResponse<OpenSubsonicArtistInfo2Response> getArtistInfo2() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicArtistInfo2Response()
                .setArtistInfo2(new OpenSubsonicArtistInfo2()));
    }

    @RequestMapping(value = "/opensubsonic/rest/scrobble.view", method = {GET, POST})
    public OpenSubsonicResponse<OpenSubsonicEmptyResponse> scrobble(@RequestParam String id, @RequestParam(defaultValue = "true") boolean submission) throws ObjectNotFoundException {
        if (submission) {
            playbackHistoryFacade.addSongToHistory(id);
        }
        return openSubsonicResponseService.createSuccessful();
    }

    @RequestMapping(value = "/opensubsonic/rest/star.view", method = {GET, POST})
    public OpenSubsonicResponse<OpenSubsonicEmptyResponse> star(@RequestParam(required = false) String id) throws ObjectNotFoundException {
        if (id != null) {
            playlistFacade.likeSong(id);
        }
        return openSubsonicResponseService.createSuccessful();
    }

    @RequestMapping(value = "/opensubsonic/rest/unstar.view", method = {GET, POST})
    public OpenSubsonicResponse<OpenSubsonicEmptyResponse> unstar(@RequestParam(required = false) String id) throws ObjectNotFoundException {
        if (id != null) {
            playlistFacade.unlikeSong(id);
        }
        return openSubsonicResponseService.createSuccessful();
    }

    @RequestMapping(value = "/opensubsonic/rest/getSong.view", method = {GET, POST})
    public OpenSubsonicResponse<OpenSubsonicSongResponse> getSong(@RequestParam(required = false) String id) throws ObjectNotFoundException {
        SongDetailsDto song = libraryFacade.getSong(id);
        PlaylistSongsDto likePlaylist = playlistFacade.getLikePlaylist();
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicSongResponse().setChild(toChild(song, likePlaylist)));
    }

    @RequestMapping(value = "/opensubsonic/rest/getPlaylists.view", method = {GET, POST})
    public OpenSubsonicResponse<OpenSubsonicPlaylistsResponse> getPlaylists() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicPlaylistsResponse()
                .setPlaylists(new OpenSubsonicPlaylistsResponse.Playlists()
                        .setPlaylist(playlistFacade.getPlaylists().stream()
                                .filter(playlist -> playlist.getType() == Playlist.Type.NORMAL)
                                .map(playlist -> new OpenSubsonicPlaylist()
                                        .setId(playlist.getId())
                                        .setName(nullToUnknown(playlist.getName()))
                                        .setCreated(formatDate(playlist.getCreationDate()))
                                        .setChanged(formatDate(playlist.getUpdateDate()))
                                )
                                .toList())));
    }

    @RequestMapping(value = "/opensubsonic/rest/getPlaylist.view", method = {GET, POST})
    public OpenSubsonicResponse<OpenSubsonicPlaylistResponse> getPlaylist(@RequestParam String id) throws ObjectNotFoundException {
        PlaylistSongsDto playlist = playlistFacade.getPlaylistById(id);
        PlaylistSongsDto likePlaylist = playlistFacade.getLikePlaylist();
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicPlaylistResponse()
                .setPlaylist(toPlaylistWithSongs(playlist, likePlaylist)));
    }

    private OpenSubsonicPlaylistWithSongs toPlaylistWithSongs(PlaylistSongsDto playlist, PlaylistSongsDto likePlaylist) {
        return new OpenSubsonicPlaylistWithSongs()
                .setId(playlist.getPlaylist().getId())
                .setName(nullToUnknown(playlist.getPlaylist().getName()))
                .setCreated(formatDate(playlist.getPlaylist().getCreationDate()))
                .setChanged(formatDate(playlist.getPlaylist().getUpdateDate()))
                .setEntry(playlist.getSongs().stream()
                        .map(playlistSong -> toChild(playlistSong.getSong(), likePlaylist))
                        .toList());
    }

    @RequestMapping(value = "/opensubsonic/rest/createPlaylist.view", method = {GET, POST})
    public OpenSubsonicResponse<OpenSubsonicPlaylistResponse> createPlaylist(
            @RequestParam String name,
            @RequestParam(required = false) String songId
    ) {
        PlaylistSongsDto playlist = playlistFacade.createNormalPlaylist(new PlaylistCreationCommandDto()
                .setName(name)
                .setSongIds(songId != null ? List.of(songId) : List.of())
        );
        PlaylistSongsDto likePlaylist = playlistFacade.getLikePlaylist();
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicPlaylistResponse()
                .setPlaylist(toPlaylistWithSongs(playlist, likePlaylist)));
    }

    @RequestMapping(value = "/opensubsonic/rest/updatePlaylist.view", method = {GET, POST})
    public OpenSubsonicResponse<OpenSubsonicEmptyResponse> updatePlaylist(
            @RequestParam String playlistId,
            @RequestParam(required = false) @Nullable String name,
            @RequestParam(required = false) @Nullable List<String> songIdToAdd,
            @RequestParam(required = false) @Nullable List<Integer> songIndexToRemove
    ) throws ObjectNotFoundException {
        PlaylistSongsDto playlist = playlistFacade.getPlaylistById(playlistId);
        PlaylistUpdateCommandDto command = new PlaylistUpdateCommandDto()
                .setId(playlist.getPlaylist().getId())
                .setOverrideName(name);
        List<PlaylistUpdateCommandDto.SongId> overridePlaylistSongIds = new ArrayList<>(playlist.getSongs().stream()
                .map(playlistSong -> new PlaylistUpdateCommandDto.SongId()
                        .setId(playlistSong.getId())
                        .setSongId(playlistSong.getSong().getSong().getId()))
                .toList());
        if (songIndexToRemove != null) {
            List<Integer> songIndexToRemoveDescending = songIndexToRemove.stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.reverseOrder())
                    .toList();
            songIndexToRemoveDescending.forEach(index -> overridePlaylistSongIds.remove((int) index));
        }
        if (songIdToAdd != null) {
            songIdToAdd.forEach(songId -> overridePlaylistSongIds.add(new PlaylistUpdateCommandDto.SongId()
                    .setSongId(songId)));
        }
        command.setOverriddenSongIds(overridePlaylistSongIds);
        playlistFacade.updatePlaylist(command);
        return openSubsonicResponseService.createSuccessful();
    }

    @RequestMapping(value = "/opensubsonic/rest/search3.view", method = {GET, POST})
    public OpenSubsonicResponse<OpenSubsonicSearch3Response> search3(
            @RequestParam String query,
            @RequestParam(defaultValue = "20") int artistCount,
            @RequestParam(defaultValue = "0") int artistOffset,
            @RequestParam(defaultValue = "20") int albumCount,
            @RequestParam(defaultValue = "0") int albumOffset,
            @RequestParam(defaultValue = "20") int songCount,
            @RequestParam(defaultValue = "0") int songOffset
    ) {
        String queryValue = query.trim()
                .replaceAll("^\"", "")
                .replaceAll("\"$", "");
        if (!queryValue.isEmpty()) {
            throw new IllegalArgumentException("Only empty query is supported.");
        }
        PlaylistSongsDto likePlaylist = playlistFacade.getLikePlaylist();
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicSearch3Response()
                .setSearchResult3(new OpenSubsonicSearchResult3()
                        .setArtist(artistCount > 0 ? libraryFacade.getArtists(artistCount, artistOffset).stream()
                                .map(this::toArtistID3)
                                .toList() : List.of())
                        .setAlbum(albumCount > 0 ? libraryFacade.getAlbums(albumCount, albumOffset).stream()
                                .map(this::toAlbumID3)
                                .toList() : List.of())
                        .setSong(songCount > 0 ? libraryFacade.getSongs(songCount, songOffset).stream()
                                .map(song -> toChild(song, likePlaylist))
                                .toList() : List.of())
                ));
    }

    @RequestMapping(value = "/opensubsonic/rest/getIndexes.view", method = {GET, POST})
    public OpenSubsonicResponse<OpenSubsonicIndexesResponse> getIndexes() {
        return openSubsonicResponseService.createSuccessful(new OpenSubsonicIndexesResponse()
                .setIndexes(new OpenSubsonicIndexes()
                        .setShortcut(List.of())
                        .setIndex(List.of())
                        .setChild(List.of())
                ));
    }
}
