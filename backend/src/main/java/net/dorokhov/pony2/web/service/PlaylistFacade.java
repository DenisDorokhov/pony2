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
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PlaylistFacade {

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
    public List<PlaylistDto> getByType(Playlist.Type type) {
        return playlistService.getByUserIdAndType(userContext.getAuthenticatedUser().getId(), type).stream()
                .map(PlaylistDto::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlaylistSongsDto getById(String id) throws ObjectNotFoundException, AccessDeniedException {
        Playlist playlist = playlistService.getById(id)
                .orElseThrow(() -> new ObjectNotFoundException(Playlist.class, id));
        if (!Objects.equals(playlist.getUser().getId(), userContext.getAuthenticatedUser().getId())) {
            throw new AccessDeniedException();
        }
        return PlaylistSongsDto.of(playlist, isAdmin());
    }

    private boolean isAdmin() {
        return userContext.getAuthenticatedUser().getRoles().contains(User.Role.ADMIN);
    }

    @Transactional
    public PlaylistSongsDto create(PlaylistCreationCommandDto command) {
        Playlist playlist = new Playlist()
                .setName(command.getName())
                .setType(command.getType());
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
        Playlist foundPlaylist = playlistService.getById(command.getId())
                .orElseThrow(() -> new ObjectNotFoundException(Playlist.class, command.getId()));
        if (!Objects.equals(foundPlaylist.getUser().getId(), userContext.getAuthenticatedUser().getId())) {
            throw new AccessDeniedException();
        }
        Playlist playlist = new Playlist()
                .setId(command.getId())
                .setName(command.getName())
                .setType(command.getType());
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

    public PlaylistSongsDto addSong(String playlistId, String songId) throws ObjectNotFoundException, AccessDeniedException {
        Playlist playlist = playlistService.getById(playlistId)
                .orElseThrow(() -> new ObjectNotFoundException(Playlist.class, playlistId));
        Song song = libraryService.getSongById(songId)
                .orElseThrow(() -> new ObjectNotFoundException(Song.class, songId));
        if (!Objects.equals(playlist.getUser().getId(), userContext.getAuthenticatedUser().getId())) {
            throw new AccessDeniedException();
        }
        AtomicInteger sort = new AtomicInteger(0);
        playlist.getSongs().forEach(playlistSong ->
                playlistSong.setSort(sort.getAndIncrement()));
        playlist.getSongs().add(new PlaylistSong()
                .setPlaylist(playlist)
                .setSort(sort.getAndIncrement())
                .setSong(song)
        );
        return null;
    }
}
