package net.dorokhov.pony2.core.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.library.service.LibraryService;
import net.dorokhov.pony2.api.library.service.PlaylistService;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.api.user.domain.UserCreatedEvent;
import net.dorokhov.pony2.api.user.domain.UserDeletingEvent;
import net.dorokhov.pony2.api.user.service.UserService;
import net.dorokhov.pony2.core.library.repository.PlaylistRepository;
import net.dorokhov.pony2.core.library.repository.SongRepository;
import net.dorokhov.pony2.core.library.service.exception.PlaylistNotFoundException;
import net.dorokhov.pony2.core.library.service.exception.SongNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PlaylistServiceImpl implements PlaylistService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final PlaylistRepository playlistRepository;
    private final SongRepository songRepository;
    private final UserService userService;
    private final LibraryService libraryService;

    public PlaylistServiceImpl(
            PlaylistRepository playlistRepository,
            SongRepository songRepository,
            UserService userService,
            LibraryService libraryService
    ) {
        this.playlistRepository = playlistRepository;
        this.songRepository = songRepository;
        this.userService = userService;
        this.libraryService = libraryService;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Playlist> getByUserId(String userId) {
        return playlistRepository.findByUserId(userId, Sort.by("type", "name"));
    }

    @Transactional(readOnly = true)
    @Override
    public List<Playlist> getByUserIdAndType(String userId, Playlist.Type type) {
        return playlistRepository.findByUserIdAndType(userId, type, Sort.by("name"));
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
        duplicateIndices.stream()
                .sorted(Collections.reverseOrder())
                .forEach(index -> savedPlaylist.getSongs().remove((int) index));
        return savedPlaylist;
    }

    @Transactional
    @Override
    public Playlist updatePlaylist(PlaylistUpdateCommand command) throws PlaylistNotFoundException {
        Playlist storedPlaylist = playlistRepository.findLockedById(command.getId())
                .orElseThrow(() -> new PlaylistNotFoundException(command.getId()));
        Playlist playlist = new Playlist()
                .setId(command.getId())
                .setCreationDate(storedPlaylist.getCreationDate())
                .setUpdateDate(LocalDateTime.now())
                .setType(storedPlaylist.getType())
                .setUser(storedPlaylist.getUser());
        if (command.getOverrideName() != null && storedPlaylist.getType() == Playlist.Type.NORMAL) {
            playlist.setName(command.getOverrideName());
        } else {
            playlist.setName(storedPlaylist.getName());
        }
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

    @Transactional(readOnly = true)
    @Override
    public String backupPlaylists() {
        Map<String, List<BackupPlaylist>> userEmailToBackup = new HashMap<>();
        for (User user : userService.getAll()) {
            List<BackupPlaylist> backup = getByUserId(user.getId()).stream()
                    .map(BackupPlaylist::of)
                    .toList();
            userEmailToBackup.put(user.getEmail(), backup);
        }
        try {
            return new ObjectMapper().writeValueAsString(userEmailToBackup);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    @Override
    public RestoredPlaylists restorePlaylists(String backup) {

        Map<String, List<BackupPlaylist>> userEmailToBackup;
        try {
            userEmailToBackup = new ObjectMapper().readValue(backup, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        List<Playlist> restoredPlaylists = new ArrayList<>();
        Set<String> notFoundUserEmails = new TreeSet<>();
        Set<String> notFoundSongs = new TreeSet<>();
        for (String userEmail : userEmailToBackup.keySet()) {
            userService.getByEmail(userEmail).ifPresentOrElse(user -> {
                for (BackupPlaylist backupPlaylist : userEmailToBackup.get(userEmail)) {
                    List<Song> songs = backupPlaylist.songPaths().stream()
                            .map(path -> {
                                Song song = songRepository.findByPath(path);
                                if (song == null) {
                                    logger.warn("Playlist '{}' will not be fully restored: song '{}' not found.", backupPlaylist.name(), song);
                                    notFoundSongs.add(path);
                                }
                                return song;
                            })
                            .filter(Objects::nonNull)
                            .toList();
                    Playlist playlist;
                    if (backupPlaylist.type() == Playlist.Type.LIKE) {
                        playlist = getByUserIdAndType(user.getId(), Playlist.Type.LIKE).getFirst();
                        playlist.setUpdateDate(LocalDateTime.now());
                        playlist.getSongs().addAll(songs.stream()
                                .map(song -> new PlaylistSong()
                                        .setPlaylist(playlist)
                                        .setSong(song)
                                )
                                .toList());
                    } else {
                        playlist = new Playlist()
                                .setName(backupPlaylist.name())
                                .setType(Playlist.Type.NORMAL)
                                .setUser(user);
                        playlist.setSongs(songs.stream()
                                .map(song -> new PlaylistSong()
                                        .setPlaylist(playlist)
                                        .setSong(song)
                                )
                                .toList());
                    }
                    restoredPlaylists.add(normalizeAndSavePlaylist(playlist));
                }
            }, () -> notFoundUserEmails.add(userEmail));
        }
        return new RestoredPlaylists(restoredPlaylists, new ArrayList<>(notFoundUserEmails), new ArrayList<>(notFoundSongs));
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

    record BackupPlaylist (
            String name,
            Playlist.Type type,
            List<String> songPaths
    ) {
        static BackupPlaylist of(Playlist playlist) {
            return new BackupPlaylist(
                    playlist.getName(), playlist.getType(),
                    playlist.getSongs().stream()
                            .map(PlaylistSong::getSong)
                            .map(Song::getPath)
                            .toList()
            );
        }
    }
}
