package net.dorokhov.pony2.web.service;

import net.dorokhov.pony2.api.library.domain.Playlist;
import net.dorokhov.pony2.api.library.domain.Song;
import net.dorokhov.pony2.api.library.service.PlaylistService;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.core.library.service.exception.PlaylistNotFoundException;
import net.dorokhov.pony2.core.library.service.exception.SongNotFoundException;
import net.dorokhov.pony2.web.dto.*;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class PlaylistFacade {

    private final PlaylistService playlistService;
    private final UserContext userContext;

    public PlaylistFacade(
            PlaylistService playlistService,
            UserContext userContext
    ) {
        this.playlistService = playlistService;
        this.userContext = userContext;
    }

    @Transactional(readOnly = true)
    public List<PlaylistDto> getPlaylists() {
        return playlistService.getByUserId(userContext.getAuthenticatedUser().getId()).stream()
                .map(PlaylistDto::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlaylistSongsDto getPlaylistById(String id) throws ObjectNotFoundException {
        Playlist playlist = getAndValidatePlaylist(id);
        return PlaylistSongsDto.of(playlist, isAdmin());
    }

    private Playlist getAndValidatePlaylist(String id) throws ObjectNotFoundException {
        Playlist playlist = playlistService.getById(id)
                .orElseThrow(() -> new ObjectNotFoundException(Playlist.class, id));
        if (!Objects.equals(playlist.getUser().getId(), userContext.getAuthenticatedUser().getId())) {
            throw new ObjectNotFoundException(Playlist.class, id);
        }
        return playlist;
    }

    private boolean isAdmin() {
        return userContext.getAuthenticatedUser().getRoles().contains(User.Role.ADMIN);
    }

    @Transactional
    public PlaylistSongsDto createNormalPlaylist(PlaylistCreationCommandDto command) {
        Playlist playlist = playlistService.createNormalPlaylist(command.convert(userContext.getAuthenticatedUser().getId()));
        return PlaylistSongsDto.of(playlist, isAdmin());
    }

    @Transactional
    public PlaylistSongsDto updatePlaylist(PlaylistUpdateCommandDto command) throws ObjectNotFoundException {
        getAndValidatePlaylist(command.getId());
        try {
            return PlaylistSongsDto.of(playlistService.updatePlaylist(command.convert()), isAdmin());
        } catch (PlaylistNotFoundException e) {
            throw new ObjectNotFoundException(Playlist.class, command.getId());
        }
    }

    @Transactional
    public PlaylistSongsDto addSongToPlaylist(String playlistId, String songId) throws ObjectNotFoundException {
        getAndValidatePlaylist(playlistId);
        try {
            return PlaylistSongsDto.of(playlistService.addSongToPlaylist(playlistId, songId), isAdmin());
        } catch (PlaylistNotFoundException e) {
            throw new ObjectNotFoundException(Playlist.class, playlistId);
        } catch (SongNotFoundException e) {
            throw new ObjectNotFoundException(Song.class, songId);
        }
    }

    @Transactional
    public PlaylistSongsDto deleteNormalPlaylist(String playlistId) throws ObjectNotFoundException {
        Playlist playlist = getAndValidatePlaylist(playlistId);
        if (playlist.getType() != Playlist.Type.NORMAL) {
            throw new ObjectNotFoundException(Playlist.class, playlistId);
        }
        PlaylistSongsDto result = PlaylistSongsDto.of(playlist, isAdmin());
        playlistService.delete(playlistId);
        return result;
    }

    @Transactional
    public PlaylistSongsDto getLikePlaylist() {
        return PlaylistSongsDto.of(getAndValidateLikePlaylist(), isAdmin());
    }

    private Playlist getAndValidateLikePlaylist() {
        List<Playlist> playlists = playlistService.getByUserIdAndType(userContext.getAuthenticatedUser().getId(), Playlist.Type.LIKE);
        if (playlists.isEmpty()) {
            throw new IllegalStateException("Like playlist of user '" + userContext.getAuthenticatedUser().getId() + "' not found.");
        }
        if (playlists.size() > 1) {
            throw new IllegalStateException("Multiple like playlists of user '" + userContext.getAuthenticatedUser().getId() + "' found.");
        }
        return playlists.getFirst();
    }

    @Transactional
    public PlaylistSongsDto likeSong(String songId) throws ObjectNotFoundException {
        Playlist playlist = getAndValidateLikePlaylist();
        try {
            return PlaylistSongsDto.of(playlistService.addSongToPlaylist(playlist.getId(), songId), isAdmin());
        } catch (PlaylistNotFoundException e) {
            throw new ObjectNotFoundException(Playlist.class, playlist.getId());
        } catch (SongNotFoundException e) {
            throw new ObjectNotFoundException(Song.class, songId);
        }
    }

    @Transactional
    public PlaylistSongsDto unlikeSong(String songId) throws ObjectNotFoundException {
        Playlist playlist = getAndValidateLikePlaylist();
        try {
            return PlaylistSongsDto.of(playlistService.removeSongFromPlaylist(playlist.getId(), songId), isAdmin());
        } catch (PlaylistNotFoundException e) {
            throw new ObjectNotFoundException(Playlist.class, playlist.getId());
        }
    }

    @Transactional
    public String backupPlaylists() {
        return playlistService.backupPlaylists();
    }

    @Transactional
    public RestoredPlaylistsDto restorePlaylists(MultipartFile backupFile) {
        String backup;
        try {
            backup = readResource(backupFile.getResource());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return RestoredPlaylistsDto.of(playlistService.restorePlaylists(backup));
    }

    private String readResource(Resource resource) throws IOException {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
    }
}
