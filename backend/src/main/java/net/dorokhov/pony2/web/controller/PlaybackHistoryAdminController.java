package net.dorokhov.pony2.web.controller;

import net.dorokhov.pony2.web.dto.BackupDto;
import net.dorokhov.pony2.web.dto.RestoredHistoryDto;
import net.dorokhov.pony2.web.service.PlaybackHistoryFacade;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(produces = APPLICATION_JSON_VALUE)
public class PlaybackHistoryAdminController implements ErrorHandlingController {

    private final PlaybackHistoryFacade playbackHistoryFacade;

    public PlaybackHistoryAdminController(PlaybackHistoryFacade playbackHistoryFacade) {
        this.playbackHistoryFacade = playbackHistoryFacade;
    }

    @GetMapping("/api/admin/history/backup")
    public BackupDto backupHistory() {
        return playbackHistoryFacade.backupHistory();
    }

    @PostMapping("/api/admin/history/restore")
    public RestoredHistoryDto restoreHistory(@RequestParam("file") MultipartFile file) {
        return playbackHistoryFacade.restoreHistory(file);
    }
}
