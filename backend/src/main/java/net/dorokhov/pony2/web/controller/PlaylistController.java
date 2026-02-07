package net.dorokhov.pony2.web.controller;

import jakarta.validation.Valid;
import net.dorokhov.pony2.web.dto.PlaylistCreationCommandDto;
import net.dorokhov.pony2.web.dto.PlaylistDto;
import net.dorokhov.pony2.web.dto.PlaylistSongsDto;
import net.dorokhov.pony2.web.dto.PlaylistUpdateCommandDto;
import net.dorokhov.pony2.web.service.PlaylistFacade;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(produces = APPLICATION_JSON_VALUE)
public class PlaylistController implements ErrorHandlingController {

    private final PlaylistFacade playlistFacade;

    public PlaylistController(PlaylistFacade playlistFacade) {
        this.playlistFacade = playlistFacade;
    }

    @GetMapping("/api/playlists")
    public List<PlaylistDto> getPlaylists() {
        return playlistFacade.getPlaylists();
    }

    @GetMapping("/api/playlists/{playlistId}")
    public PlaylistSongsDto getPlaylistById(@PathVariable String playlistId) throws ObjectNotFoundException {
        return playlistFacade.getPlaylistById(playlistId);
    }

    @PutMapping("/api/playlists")
    public PlaylistSongsDto updatePlaylist(@Valid @RequestBody PlaylistUpdateCommandDto command) throws ObjectNotFoundException {
        return playlistFacade.updatePlaylist(command);
    }

    @PostMapping("/api/playlists/{playlistId}/songs/{songId}")
    public PlaylistSongsDto addSongToPlaylists(@PathVariable String playlistId, @PathVariable String songId) throws ObjectNotFoundException {
        return playlistFacade.addSongToPlaylist(playlistId, songId);
    }

    @PostMapping("/api/playlists/{playlistId}/songs")
    public PlaylistSongsDto addSongToPlaylists(@PathVariable String playlistId, @RequestBody List<String> songIds) throws ObjectNotFoundException {
        return playlistFacade.addSongsToPlaylist(playlistId, songIds);
    }

    @PostMapping("/api/playlists/normal")
    public PlaylistSongsDto createNormalPlaylist(@Valid @RequestBody PlaylistCreationCommandDto command) {
        return playlistFacade.createNormalPlaylist(command);
    }

    @DeleteMapping("/api/playlists/normal/{playlistId}")
    public PlaylistSongsDto deleteNormalPlaylist(@PathVariable String playlistId) throws ObjectNotFoundException {
        return playlistFacade.deleteNormalPlaylist(playlistId);
    }

    @GetMapping("/api/playlists/like")
    public PlaylistSongsDto getLikePlaylist() {
        return playlistFacade.getLikePlaylist();
    }

    @PostMapping("/api/playlists/like/songs/{songId}")
    public PlaylistSongsDto likeSong(@PathVariable String songId) throws ObjectNotFoundException {
        return playlistFacade.likeSong(songId);
    }

    @DeleteMapping("/api/playlists/like/songs/{songId}")
    public PlaylistSongsDto unlikeSong(@PathVariable String songId) throws ObjectNotFoundException {
        return playlistFacade.unlikeSong(songId);
    }
}
