package net.dorokhov.pony2.api.library.service;

import net.dorokhov.pony2.api.library.domain.Playlist;

import java.util.List;
import java.util.Optional;

public interface PlaylistService {
    List<Playlist> getByUserIdAndType(String userId, Playlist.Type type);
    Optional<Playlist> getById(String id);
    Playlist save(Playlist playlist);
}
