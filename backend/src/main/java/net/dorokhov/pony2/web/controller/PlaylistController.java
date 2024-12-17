package net.dorokhov.pony2.web.controller;

import jakarta.validation.Valid;
import net.dorokhov.pony2.api.library.domain.Playlist;
import net.dorokhov.pony2.web.dto.PlaylistCreationCommandDto;
import net.dorokhov.pony2.web.dto.PlaylistDto;
import net.dorokhov.pony2.web.dto.PlaylistSongsDto;
import net.dorokhov.pony2.web.dto.PlaylistUpdateCommandDto;
import net.dorokhov.pony2.web.service.PlaylistFacade;
import net.dorokhov.pony2.web.service.exception.AccessDeniedException;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(produces = APPLICATION_JSON_VALUE)
public class PlaylistController {

    private final PlaylistFacade playlistFacade;

    public PlaylistController(PlaylistFacade playlistFacade) {
        this.playlistFacade = playlistFacade;
    }

    @GetMapping("/api/playlists")
    public List<PlaylistDto> getByType(@RequestParam(required = false) Playlist.Type type) {
        if (type != null) {
            return playlistFacade.getByType(type);
        } else {
            return playlistFacade.getAll();
        }
    }

    @GetMapping("/api/playlists/{playlistId}")
    public PlaylistSongsDto getByType(@PathVariable String playlistId) throws AccessDeniedException, ObjectNotFoundException {
        return playlistFacade.getById(playlistId);
    }

    @DeleteMapping("/api/playlists/{playlistId}")
    public PlaylistSongsDto delete(@PathVariable String playlistId) throws AccessDeniedException, ObjectNotFoundException {
        return playlistFacade.delete(playlistId);
    }

    @PostMapping("/api/playlists")
    public PlaylistSongsDto create(@Valid @RequestBody PlaylistCreationCommandDto command) {
        return playlistFacade.create(command);
    }

    @PutMapping("/api/playlists")
    public PlaylistSongsDto update(@Valid @RequestBody PlaylistUpdateCommandDto command) throws AccessDeniedException, ObjectNotFoundException {
        return playlistFacade.update(command);
    }

    @PostMapping("/api/playlists/{playlistId}/addSong/{songId}")
    public PlaylistSongsDto addSong(@PathVariable String playlistId, @PathVariable String songId) throws AccessDeniedException, ObjectNotFoundException {
        return playlistFacade.addSong(playlistId, songId);
    }

    @PostMapping("/api/playlists/likeSong/{songId}")
    public PlaylistSongsDto likeSong(@PathVariable String songId) throws ObjectNotFoundException {
        return playlistFacade.likeSong(songId);
    }

    @PostMapping("/api/playlists/addSongToHistory/{songId}")
    public PlaylistSongsDto addSongToHistory(@PathVariable String songId) throws ObjectNotFoundException {
        return playlistFacade.addToHistory(songId);
    }
}
