package net.dorokhov.pony2.api.library.service;

import net.dorokhov.pony2.api.library.domain.Playlist;
import net.dorokhov.pony2.api.library.domain.PlaylistCreationCommand;
import net.dorokhov.pony2.api.library.domain.PlaylistUpdateCommand;
import net.dorokhov.pony2.core.library.service.exception.PlaylistNotFoundException;
import net.dorokhov.pony2.core.library.service.exception.SongNotFoundException;

import java.util.List;
import java.util.Optional;

public interface PlaylistService {
    List<Playlist> getByUserIdAndType(String userId, Playlist.Type type);
    Optional<Playlist> getById(String id);
    Playlist createNormalPlaylist(PlaylistCreationCommand command);
    Playlist updateNormalPlaylist(PlaylistUpdateCommand command) throws PlaylistNotFoundException;
    Playlist addSongToPlaylist(String id, String songId) throws PlaylistNotFoundException, SongNotFoundException;
    Playlist removeSongFromPlaylist(String id, String songId) throws PlaylistNotFoundException;
    void delete(String id);
}