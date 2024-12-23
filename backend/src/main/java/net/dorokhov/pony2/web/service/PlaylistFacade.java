package net.dorokhov.pony2.web.service;

import net.dorokhov.pony2.api.library.domain.Playlist;
import net.dorokhov.pony2.api.library.domain.Song;
import net.dorokhov.pony2.api.library.service.PlaylistService;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.core.library.service.exception.PlaylistNotFoundException;
import net.dorokhov.pony2.core.library.service.exception.SongNotFoundException;
import net.dorokhov.pony2.web.dto.PlaylistCreationCommandDto;
import net.dorokhov.pony2.web.dto.PlaylistDto;
import net.dorokhov.pony2.web.dto.PlaylistSongsDto;
import net.dorokhov.pony2.web.dto.PlaylistUpdateCommandDto;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

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
    public List<PlaylistDto> getPlaylistsByType(Playlist.Type type) {
        return playlistService.getByUserIdAndType(userContext.getAuthenticatedUser().getId(), type).stream()
                .map(PlaylistDto::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlaylistSongsDto getNormalPlaylistById(String id) throws ObjectNotFoundException {
        Playlist playlist = getAndValidateNormalPlaylist(id);
        return PlaylistSongsDto.of(playlist, isAdmin());
    }

    private Playlist getAndValidateNormalPlaylist(String id) throws ObjectNotFoundException {
        Playlist playlist = playlistService.getById(id)
                .orElseThrow(() -> new ObjectNotFoundException(Playlist.class, id));
        if (playlist.getType() != Playlist.Type.NORMAL) {
            throw new ObjectNotFoundException(Playlist.class, id);
        }
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
    public PlaylistSongsDto updateNormalPlaylist(PlaylistUpdateCommandDto command) throws ObjectNotFoundException {
        getAndValidateNormalPlaylist(command.getId());
        try {
            return PlaylistSongsDto.of(playlistService.updateNormalPlaylist(command.convert()), isAdmin());
        } catch (PlaylistNotFoundException e) {
            throw new ObjectNotFoundException(Playlist.class, command.getId());
        }
    }

    @Transactional
    public PlaylistSongsDto addSongToNormalPlaylist(String playlistId, String songId) throws ObjectNotFoundException {
        getAndValidateNormalPlaylist(playlistId);
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
        PlaylistSongsDto result = PlaylistSongsDto.of(getAndValidateNormalPlaylist(playlistId), isAdmin());
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
}
