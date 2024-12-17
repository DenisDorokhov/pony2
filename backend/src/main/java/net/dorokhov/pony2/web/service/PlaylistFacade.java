package net.dorokhov.pony2.web.service;

import net.dorokhov.pony2.api.library.domain.Playlist;
import net.dorokhov.pony2.api.library.domain.PlaylistSong;
import net.dorokhov.pony2.api.library.domain.Song;
import net.dorokhov.pony2.api.library.service.LibraryService;
import net.dorokhov.pony2.api.library.service.PlaylistService;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.web.dto.*;
import net.dorokhov.pony2.web.service.exception.AccessDeniedException;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Service
public class PlaylistFacade {

    private static final int MAX_HISTORY_PLAYLIST_SIZE = 300;

    private final PlaylistService playlistService;
    private final LibraryService libraryService;
    private final UserContext userContext;

    public PlaylistFacade(
            PlaylistService playlistService,
            LibraryService libraryService,
            UserContext userContext
    ) {
        this.playlistService = playlistService;
        this.libraryService = libraryService;
        this.userContext = userContext;
    }

    @Transactional(readOnly = true)
    public List<PlaylistDto> getAll() {
        return playlistService.getByUserId(userContext.getAuthenticatedUser().getId()).stream()
                .map(PlaylistDto::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlaylistDto> getByType(Playlist.Type type) {
        return playlistService.getByUserIdAndType(userContext.getAuthenticatedUser().getId(), type).stream()
                .map(PlaylistDto::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlaylistSongsDto getById(String id) throws ObjectNotFoundException, AccessDeniedException {
        Playlist playlist = getAndValidatePlaylist(id, playlistService::getById);
        return PlaylistSongsDto.of(playlist, isAdmin());
    }

    private Playlist getAndValidatePlaylist(String id, Function<String, Optional<Playlist>> fetcher) throws ObjectNotFoundException, AccessDeniedException {
        Playlist playlist = fetcher.apply(id)
                .orElseThrow(() -> new ObjectNotFoundException(Playlist.class, id));
        if (!Objects.equals(playlist.getUser().getId(), userContext.getAuthenticatedUser().getId())) {
            throw new AccessDeniedException();
        }
        return playlist;
    }

    private boolean isAdmin() {
        return userContext.getAuthenticatedUser().getRoles().contains(User.Role.ADMIN);
    }

    @Transactional
    public PlaylistSongsDto create(PlaylistCreationCommandDto command) {
        Playlist playlist = new Playlist()
                .setName(command.getName())
                .setType(Playlist.Type.NORMAL);
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
        return PlaylistSongsDto.of(playlistService.save(playlist), isAdmin());
    }

    @Transactional
    public PlaylistSongsDto update(PlaylistUpdateCommandDto command) throws ObjectNotFoundException, AccessDeniedException {
        Playlist storedPlaylist = getAndValidatePlaylist(command.getId(), playlistService::lockById);
        if (storedPlaylist.getType() != Playlist.Type.NORMAL) {
            throw new AccessDeniedException();
        }
        Playlist playlist = new Playlist()
                .setId(command.getId())
                .setName(command.getName())
                .setType(storedPlaylist.getType());
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
        return PlaylistSongsDto.of(playlistService.save(playlist), isAdmin());
    }

    @Transactional
    public PlaylistSongsDto addSong(String playlistId, String songId) throws ObjectNotFoundException, AccessDeniedException {
        Playlist playlist = getAndValidatePlaylist(playlistId, playlistService::lockById);
        return PlaylistSongsDto.of(addToPlaylist(playlist, songId), isAdmin());
    }

    private Playlist addToPlaylist(Playlist playlist, String songId) throws ObjectNotFoundException {
        Song song = libraryService.getSongById(songId)
                .orElseThrow(() -> new ObjectNotFoundException(Song.class, songId));
        AtomicInteger sort = new AtomicInteger(0);
        playlist.getSongs().forEach(playlistSong ->
                playlistSong.setSort(sort.getAndIncrement()));
        playlist.getSongs().add(new PlaylistSong()
                .setPlaylist(playlist)
                .setSort(sort.getAndIncrement())
                .setSong(song)
        );
        return playlistService.save(playlist);
    }

    @Transactional
    public PlaylistSongsDto likeSong(String songId) throws ObjectNotFoundException {
        return PlaylistSongsDto.of(addToPlaylist(Playlist.Type.LIKE, songId), isAdmin());
    }

    private Playlist addToPlaylist(Playlist.Type type, String songId) throws ObjectNotFoundException {
        Playlist playlist = playlistService.lockOneByType(userContext.getAuthenticatedUser().getId(), type);
        return addToPlaylist(playlist, songId);
    }

    @Transactional
    public PlaylistSongsDto addToHistory(String songId) throws ObjectNotFoundException {
        Playlist playlist = addToPlaylist(Playlist.Type.HISTORY, songId);
        if (playlist.getSongs().size() > MAX_HISTORY_PLAYLIST_SIZE) {
            for (int i = 0; i < MAX_HISTORY_PLAYLIST_SIZE; i++) {
                playlist.getSongs().removeFirst();
            }
            AtomicInteger sort = new AtomicInteger(0);
            playlist.getSongs().forEach(playlistSong ->
                    playlistSong.setSort(sort.getAndIncrement()));
            playlistService.save(playlist);
        }
        return PlaylistSongsDto.of(playlist, isAdmin());
    }

    @Transactional
    public PlaylistSongsDto delete(String playlistId) throws ObjectNotFoundException, AccessDeniedException {
        PlaylistSongsDto result = PlaylistSongsDto.of(getAndValidatePlaylist(playlistId, playlistService::lockById), isAdmin());
        playlistService.delete(playlistId);
        return result;
    }
}
