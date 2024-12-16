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

    @GetMapping("/api/playlist/list/{type}")
    public List<PlaylistDto> getByType(@PathVariable Playlist.Type type) {
        return playlistFacade.getByType(type);
    }

    @GetMapping("/api/playlist/{playlistId}")
    public PlaylistSongsDto getByType(@PathVariable String playlistId) throws AccessDeniedException, ObjectNotFoundException {
        return playlistFacade.getById(playlistId);
    }

    @PostMapping("/api/playlist")
    public PlaylistSongsDto create(@Valid @RequestBody PlaylistCreationCommandDto command) {
        return playlistFacade.create(command);
    }

    @PutMapping("/api/playlist")
    public PlaylistSongsDto update(@Valid @RequestBody PlaylistUpdateCommandDto command) throws AccessDeniedException, ObjectNotFoundException {
        return playlistFacade.update(command);
    }

    @PostMapping("/api/playlist/{playlistId}/addSong/{songId}")
    public PlaylistSongsDto addSong(@PathVariable String playlistId, @PathVariable String songId) throws AccessDeniedException, ObjectNotFoundException {
        return playlistFacade.addSong(playlistId, songId);
    }
}
