package net.dorokhov.pony2.api.library.service;

import net.dorokhov.pony2.api.library.domain.PlaybackHistorySong;
import net.dorokhov.pony2.api.library.domain.PlaybackHistoryStatistics;
import net.dorokhov.pony2.core.library.service.exception.SongNotFoundException;

import java.util.List;

public interface PlaybackHistoryService {
    PlaybackHistoryStatistics getStatistics(String userId);
    List<PlaybackHistorySong> getHistory(String userId);
    PlaybackHistorySong addSongToHistory(String userId, String songId) throws SongNotFoundException;
}
