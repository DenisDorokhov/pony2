package net.dorokhov.pony2.core.library.service;

import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.library.service.LibraryService;
import net.dorokhov.pony2.api.library.service.PlaylistService;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.api.user.domain.UserCreatedEvent;
import net.dorokhov.pony2.api.user.domain.UserDeletingEvent;
import net.dorokhov.pony2.api.user.service.UserService;
import net.dorokhov.pony2.core.library.repository.PlaylistRepository;
import net.dorokhov.pony2.core.library.service.exception.PlaylistNotFoundException;
import net.dorokhov.pony2.core.library.service.exception.SongNotFoundException;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PlaylistServiceImpl implements PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final UserService userService;
    private final LibraryService libraryService;

    public PlaylistServiceImpl(
            PlaylistRepository playlistRepository,
            UserService userService,
            LibraryService libraryService
    ) {
        this.playlistRepository = playlistRepository;
        this.userService = userService;
        this.libraryService = libraryService;
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

    @Transactional
    @Override
    public Playlist createNormalPlaylist(PlaylistCreationCommand command) {
        Playlist playlist = new Playlist()
                .setName(command.getName())
                .setType(Playlist.Type.NORMAL)
                .setUser(userService.getById(command.getUserId())
                        .orElseThrow(() -> new IllegalArgumentException("User '" + command.getUserId() + "' not found.")));
        AtomicInteger sort = new AtomicInteger(0);
        playlist.setSongs(command.getSongIds().stream()
                .flatMap(songId -> libraryService.getSongById(songId)
                        .map(song -> new PlaylistSong()
                                .setPlaylist(playlist)
                                .setSort(sort.getAndIncrement())
                                .setSong(song)
                        )
                        .stream())
                .toList());
        return playlistRepository.save(normalizePlaylist(playlist));
    }

    private Playlist normalizePlaylist(Playlist playlist) {
        if (playlist.getType() == Playlist.Type.LIKE) {
            // TODO: implement
        }
        if (playlist.getType() == Playlist.Type.HISTORY) {
            // TODO: implement
        }
        return playlist;
    }

    @Transactional
    @Override
    public Playlist updateNormalPlaylist(PlaylistUpdateCommand command) throws PlaylistNotFoundException {
        Playlist storedPlaylist = playlistRepository.findLockedById(command.getId())
                .orElseThrow(() -> new PlaylistNotFoundException(command.getId()));
        Playlist playlist = new Playlist()
                .setId(command.getId())
                .setCreationDate(storedPlaylist.getCreationDate())
                .setUpdateDate(LocalDateTime.now())
                .setName(command.getName())
                .setType(storedPlaylist.getType())
                .setUser(storedPlaylist.getUser());
        AtomicInteger sort = new AtomicInteger(0);
        playlist.setSongs(command.getSongIds().stream()
                .flatMap(songId -> libraryService.getSongById(songId.getSongId())
                        .map(song -> new PlaylistSong()
                                .setId(songId.getId())
                                .setPlaylist(playlist)
                                .setSort(sort.getAndIncrement())
                                .setSong(song)
                        )
                        .stream()
                )
                .toList());
        return playlistRepository.save(normalizePlaylist(playlist));
    }

    @Transactional
    @Override
    public Playlist addSongToPlaylist(String id, String songId) throws PlaylistNotFoundException, SongNotFoundException {
        Playlist playlist = playlistRepository.findLockedById(id)
                .orElseThrow(() -> new PlaylistNotFoundException(id));
        playlist.setUpdateDate(LocalDateTime.now());
        Song song = libraryService.getSongById(songId)
                .orElseThrow(() -> new SongNotFoundException(songId));
        AtomicInteger sort = new AtomicInteger(0);
        playlist.getSongs().forEach(playlistSong ->
                playlistSong.setSort(sort.getAndIncrement()));
        playlist.getSongs().add(new PlaylistSong()
                .setPlaylist(playlist)
                .setSort(sort.getAndIncrement())
                .setSong(song)
        );
        return playlistRepository.save(normalizePlaylist(playlist));
    }

    @Transactional
    @Override
    public Playlist removeSongFromPlaylist(String id, String songId) throws PlaylistNotFoundException {
        Playlist playlist = playlistRepository.findLockedById(id)
                .orElseThrow(() -> new PlaylistNotFoundException(id));
        playlist.getSongs().removeIf(playlistSong -> playlistSong.getSong().getId().equals(songId));
        return playlistRepository.save(normalizePlaylist(playlist));
    }

    @Transactional
    @Override
    public void delete(String id) {
        playlistRepository.findLockedById(id).ifPresent(playlistRepository::delete);
    }

    @Transactional
    @EventListener(UserCreatedEvent.class)
    public void onUserCreated(UserCreatedEvent event) {
        User user = userService.getById(event.getUserId()).orElseThrow();
        playlistRepository.save(new Playlist()
                .setType(Playlist.Type.HISTORY)
                .setUser(user));
        playlistRepository.save(new Playlist()
                .setType(Playlist.Type.LIKE)
                .setUser(user));
    }

    @Transactional
    @EventListener(UserDeletingEvent.class)
    public void onUserDeleting(UserDeletingEvent event) {
        playlistRepository.deleteByUserId(event.getUserId());
    }
}
