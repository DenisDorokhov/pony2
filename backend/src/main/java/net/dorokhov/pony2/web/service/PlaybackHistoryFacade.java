package net.dorokhov.pony2.web.service;

import net.dorokhov.pony2.api.library.domain.Song;
import net.dorokhov.pony2.api.library.service.PlaybackHistoryService;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.core.library.service.exception.SongNotFoundException;
import net.dorokhov.pony2.web.dto.PlaybackHistoryDto;
import net.dorokhov.pony2.web.dto.PlaybackHistorySongDto;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaybackHistoryFacade {

    private final PlaybackHistoryService playbackHistoryService;
    private final UserContext userContext;

    public PlaybackHistoryFacade(
            PlaybackHistoryService playbackHistoryService,
            UserContext userContext
    ) {
        this.playbackHistoryService = playbackHistoryService;
        this.userContext = userContext;
    }

    @Transactional(readOnly = true)
    public PlaybackHistoryDto getHistory() {
        String userId = userContext.getAuthenticatedUser().getId();
        return new PlaybackHistoryDto()
                .setStatistics(PlaybackHistoryDto.Statistics.of(playbackHistoryService.getStatistics(userId)))
                .setSongs(playbackHistoryService.getHistory(userId).stream()
                        .map(playbackHistorySong -> PlaybackHistorySongDto.of(playbackHistorySong, isAdmin()))
                        .toList());
    }

    private boolean isAdmin() {
        return userContext.getAuthenticatedUser().getRoles().contains(User.Role.ADMIN);
    }

    @Transactional
    public PlaybackHistorySongDto addSongToHistory(String songId) throws ObjectNotFoundException {
        try {
            return PlaybackHistorySongDto.of(playbackHistoryService.addSongToHistory(userContext.getAuthenticatedUser().getId(), songId), isAdmin());
        } catch (SongNotFoundException e) {
            throw new ObjectNotFoundException(Song.class, songId);
        }
    }
}
