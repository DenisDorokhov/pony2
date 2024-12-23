package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.web.controller.common.ErrorHandlingController;
import net.dorokhov.pony2.web.dto.PlaybackHistorySongDto;
import net.dorokhov.pony2.web.service.PlaybackHistoryFacade;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(produces = APPLICATION_JSON_VALUE)
public class PlaybackHistoryController implements ErrorHandlingController {

    private final PlaybackHistoryFacade playbackHistoryFacade;

    public PlaybackHistoryController(PlaybackHistoryFacade playbackHistoryFacade) {
        this.playbackHistoryFacade = playbackHistoryFacade;
    }

    @GetMapping("/api/history")
    public List<PlaybackHistorySongDto> getHistory() {
        return playbackHistoryFacade.getHistory();
    }

    @PostMapping("/api/history/{songId}")
    public PlaybackHistorySongDto addSongToHistory(@PathVariable String songId) throws ObjectNotFoundException {
        return playbackHistoryFacade.addSongToHistory(songId);
    }
}
