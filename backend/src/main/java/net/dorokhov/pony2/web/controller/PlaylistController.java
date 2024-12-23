package net.dorokhov.pony2.web.controller;

import jakarta.validation.Valid;
import net.dorokhov.pony2.api.library.domain.Playlist;
import net.dorokhov.pony2.web.controller.common.ErrorHandlingController;
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

    @GetMapping("/api/playlists/normal")
    public List<PlaylistDto> getNormalPlaylists() {
        return playlistFacade.getPlaylistsByType(Playlist.Type.NORMAL);
    }

    @GetMapping("/api/playlists/normal/{playlistId}")
    public PlaylistSongsDto getNormalPlaylistById(@PathVariable String playlistId) throws ObjectNotFoundException {
        return playlistFacade.getNormalPlaylistById(playlistId);
    }

    @PostMapping("/api/playlists/normal")
    public PlaylistSongsDto createNormalPlaylists(@Valid @RequestBody PlaylistCreationCommandDto command) {
        return playlistFacade.createNormalPlaylist(command);
    }

    @PutMapping("/api/playlists/normal")
    public PlaylistSongsDto updateNormalPlaylists(@Valid @RequestBody PlaylistUpdateCommandDto command) throws ObjectNotFoundException {
        return playlistFacade.updateNormalPlaylist(command);
    }

    @PostMapping("/api/playlists/normal/{playlistId}/songs/{songId}")
    public PlaylistSongsDto addSongToNormalPlaylists(@PathVariable String playlistId, @PathVariable String songId) throws ObjectNotFoundException {
        return playlistFacade.addSongToNormalPlaylist(playlistId, songId);
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
