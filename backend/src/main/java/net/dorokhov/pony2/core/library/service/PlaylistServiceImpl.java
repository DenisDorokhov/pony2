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

    @Override
    @Transactional(readOnly = true)
    public List<Playlist> getByUserIdAndType(String userId, Playlist.Type type) {
        return playlistRepository.findByUserIdAndType(userId, type);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Playlist> getById(String id) {
        return playlistRepository.findById(id);
    }

    @Override
    @Transactional
    public Playlist save(Playlist playlist) {
        return playlistRepository.save(playlist);
    }
}
