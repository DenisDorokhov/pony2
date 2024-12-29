package net.dorokhov.pony2.web.controller;

import net.dorokhov.pony2.web.dto.RestoredPlaylistsDto;
import net.dorokhov.pony2.web.service.PlaylistFacade;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(produces = APPLICATION_JSON_VALUE)
public class PlaylistAdminController {

    private final PlaylistFacade playlistFacade;

    public PlaylistAdminController(PlaylistFacade playlistFacade) {
        this.playlistFacade = playlistFacade;
    }

    @GetMapping("/api/admin/playlists/backup/{userId}")
    public String backup(@PathVariable String userId) {
        return playlistFacade.backupPlaylists(userId);
    }

    @PostMapping("/api/admin/playlists/restore/{userId}")
    public RestoredPlaylistsDto backup(@PathVariable String userId, @RequestParam("file") MultipartFile file) {
        return playlistFacade.restorePlaylists(userId, file);
    }
}
