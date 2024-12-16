package net.dorokhov.pony2.core.library.service;

import net.dorokhov.pony2.api.library.domain.Playlist;
import net.dorokhov.pony2.api.library.service.PlaylistService;
import net.dorokhov.pony2.core.library.repository.PlaylistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PlaylistServiceImpl implements PlaylistService {

    private final PlaylistRepository playlistRepository;

    public PlaylistServiceImpl(PlaylistRepository playlistRepository) {
        this.playlistRepository = playlistRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Playlist> getByUserId(String userId) {
        return playlistRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    @Override
    public List<Playlist> getByUserIdAndType(String userId, Playlist.Type type) {
        return playlistRepository.findByUserIdAndType(userId, type);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<Playlist> getById(String id) {
        return playlistRepository.findById(id);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<Playlist> lockById(String id) {
        return playlistRepository.findLockedById(id);
    }

    @Transactional
    @Override
    public Playlist save(Playlist playlist) {
        return playlistRepository.save(playlist);
    }

    @Override
    public void deleteByUserId(String userId) {
        playlistRepository.deleteByUserId(userId);
    }

    @Transactional(readOnly = true)
    @Override
    public Playlist lockOneByType(String userId, Playlist.Type type) {
        List<Playlist> playlists = playlistRepository.findByUserIdAndType(userId, type);
        if (playlists.isEmpty()) {
            throw new IllegalStateException("Playlist of type '" + type + "' not found.");
        }
        if (playlists.size() > 1) {
            throw new IllegalStateException("Multiple playlists of type '" + type + "' found.");
        }
        return playlistRepository.findLockedById(playlists.getFirst().getId())
                .orElseThrow(() -> new IllegalStateException("Playlist of type '" + type + "' not found."));
    }
}
