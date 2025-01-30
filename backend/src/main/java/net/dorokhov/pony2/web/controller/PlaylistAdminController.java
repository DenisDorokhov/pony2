package net.dorokhov.pony2.web.controller;

import net.dorokhov.pony2.web.controller.common.ErrorHandlingController;
import net.dorokhov.pony2.web.dto.BackupDto;
import net.dorokhov.pony2.web.dto.RestoredPlaylistsDto;
import net.dorokhov.pony2.web.service.PlaylistFacade;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(produces = APPLICATION_JSON_VALUE)
public class PlaylistAdminController implements ErrorHandlingController {

    private final PlaylistFacade playlistFacade;

    public PlaylistAdminController(PlaylistFacade playlistFacade) {
        this.playlistFacade = playlistFacade;
    }

    @GetMapping("/api/admin/playlists/backup")
    public BackupDto backup() {
        return playlistFacade.backupPlaylists();
    }

    @PostMapping("/api/admin/playlists/restore")
    public RestoredPlaylistsDto restore(@RequestParam("file") MultipartFile file) {
        return playlistFacade.restorePlaylists(file);
    }
}
