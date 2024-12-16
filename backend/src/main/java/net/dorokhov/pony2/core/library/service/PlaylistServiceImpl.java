package net.dorokhov.pony2.core.library.service;

import net.dorokhov.pony2.api.library.domain.Playlist;
import net.dorokhov.pony2.api.library.service.PlaylistService;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.api.user.domain.UserCreatedEvent;
import net.dorokhov.pony2.api.user.domain.UserDeletingEvent;
import net.dorokhov.pony2.api.user.service.UserService;
import net.dorokhov.pony2.core.library.repository.PlaylistRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PlaylistServiceImpl implements PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final UserService userService;

    public PlaylistServiceImpl(
            PlaylistRepository playlistRepository,
            UserService userService
    ) {
        this.playlistRepository = playlistRepository;
        this.userService = userService;
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

    @Transactional
    @EventListener(UserCreatedEvent.class)
    public void onUserCreated(UserCreatedEvent event) {
        User user = userService.getById(event.getUserId()).orElseThrow();
        playlistRepository.save(new Playlist()
                .setName(Playlist.Type.HISTORY.name())
                .setType(Playlist.Type.HISTORY)
                .setUser(user));
        playlistRepository.save(new Playlist()
                .setName(Playlist.Type.LIKE.name())
                .setType(Playlist.Type.LIKE)
                .setUser(user));
    }

    @Transactional
    @EventListener(UserDeletingEvent.class)
    public void onUserDeleting(UserDeletingEvent event) {
        playlistRepository.deleteByUserId(event.getUserId());
    }
}
