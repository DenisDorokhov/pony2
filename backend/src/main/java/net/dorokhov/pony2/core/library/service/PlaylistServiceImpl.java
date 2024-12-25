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
import java.util.*;

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
        return playlistRepository.findByUserIdAndTypeOrderByName(userId, type);
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
        playlist.setSongs(command.getSongIds().stream()
                .flatMap(songId -> libraryService.getSongById(songId)
                        .map(song -> new PlaylistSong()
                                .setPlaylist(playlist)
                                .setSong(song)
                        )
                        .stream())
                .toList());
        return normalizeAndSavePlaylist(playlist);
    }

    private Playlist normalizeAndSavePlaylist(Playlist playlist) {
        Playlist savedPlaylist = playlistRepository.save(playlist);
        int sort = 0;
        Set<String> songIds = new HashSet<>();
        Set<Integer> duplicateIndices = new HashSet<>();
        for (int i = 0; i < savedPlaylist.getSongs().size(); i++) {
            PlaylistSong playlistSong = savedPlaylist.getSongs().get(i);
            playlistSong.setSort(sort++);
            if (songIds.contains(playlistSong.getSong().getId())) {
                duplicateIndices.add(i);
            }
            songIds.add(playlistSong.getSong().getId());
        }
        if (savedPlaylist.getType() == Playlist.Type.LIKE) {
            duplicateIndices.stream()
                    .sorted(Collections.reverseOrder())
                    .forEach(index -> savedPlaylist.getSongs().remove((int) index));
        }
        return savedPlaylist;
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
        if (command.getOverriddenSongIds() == null) {
            playlist.setSongs(storedPlaylist.getSongs());
        } else {
            playlist.setSongs(command.getOverriddenSongIds().stream()
                    .flatMap(songId -> libraryService.getSongById(songId.getSongId())
                            .map(song -> {
                                PlaylistSong playlistSong = null;
                                if (songId.getId() != null) {
                                    playlistSong = storedPlaylist.getSongs().stream()
                                            .filter(next -> next.getId().equals(songId.getId()))
                                            .findFirst()
                                            .orElse(null);
                                }
                                return new PlaylistSong()
                                        .setId(playlistSong != null ? playlistSong.getId() : null)
                                        .setCreationDate(playlistSong != null ? playlistSong.getCreationDate() : null)
                                        .setPlaylist(playlist)
                                        .setSong(song);
                            })
                            .stream()
                    )
                    .toList());
        }
        return normalizeAndSavePlaylist(playlist);
    }

    @Transactional
    @Override
    public Playlist addSongToPlaylist(String id, String songId) throws PlaylistNotFoundException, SongNotFoundException {
        Playlist playlist = playlistRepository.findLockedById(id)
                .orElseThrow(() -> new PlaylistNotFoundException(id));
        playlist.setUpdateDate(LocalDateTime.now());
        Song song = libraryService.getSongById(songId)
                .orElseThrow(() -> new SongNotFoundException(songId));
        playlist.getSongs().add(new PlaylistSong()
                .setPlaylist(playlist)
                .setSong(song)
        );
        return normalizeAndSavePlaylist(playlist);
    }

    @Transactional
    @Override
    public Playlist removeSongFromPlaylist(String id, String songId) throws PlaylistNotFoundException {
        Playlist playlist = playlistRepository.findLockedById(id)
                .orElseThrow(() -> new PlaylistNotFoundException(id));
        playlist.getSongs().removeIf(playlistSong -> playlistSong.getSong().getId().equals(songId));
        return normalizeAndSavePlaylist(playlist);
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
                .setType(Playlist.Type.LIKE)
                .setUser(user));
    }

    @Transactional
    @EventListener(UserDeletingEvent.class)
    public void onUserDeleting(UserDeletingEvent event) {
        playlistRepository.deleteByUserId(event.getUserId());
    }
}
