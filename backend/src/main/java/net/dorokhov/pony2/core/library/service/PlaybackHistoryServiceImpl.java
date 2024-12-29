package net.dorokhov.pony2.core.library.service;

import net.dorokhov.pony2.api.library.domain.PlaybackHistorySong;
import net.dorokhov.pony2.api.library.domain.PlaybackHistoryStatistics;
import net.dorokhov.pony2.api.library.domain.Song;
import net.dorokhov.pony2.api.library.service.LibraryService;
import net.dorokhov.pony2.api.library.service.PlaybackHistoryService;
import net.dorokhov.pony2.api.user.service.UserService;
import net.dorokhov.pony2.core.library.repository.PlaybackHistorySongRepository;
import net.dorokhov.pony2.core.library.service.exception.SongNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlaybackHistoryServiceImpl implements PlaybackHistoryService {

    private static final int MAX_HISTORY_ENTRIES = 300;

    private final PlaybackHistorySongRepository playbackHistorySongRepository;
    private final LibraryService libraryService;
    private final UserService userService;

    public PlaybackHistoryServiceImpl(
            PlaybackHistorySongRepository playbackHistorySongRepository,
            LibraryService libraryService,
            UserService userService
    ) {
        this.playbackHistorySongRepository = playbackHistorySongRepository;
        this.libraryService = libraryService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    @Override
    public PlaybackHistoryStatistics getStatistics(String userId) {
        return new PlaybackHistoryStatistics()
                .setTotalCount(playbackHistorySongRepository.countByUserId(userId));
    }

    @Transactional(readOnly = true)
    @Override
    public List<PlaybackHistorySong> getHistory(String userId) {
        return playbackHistorySongRepository.findByUserId(userId, PageRequest.of(0, MAX_HISTORY_ENTRIES,
                        Sort.by(Sort.Direction.DESC, "creationDate")))
                .getContent();
    }

    @Transactional
    @Override
    public PlaybackHistorySong addSongToHistory(String userId, String songId) throws SongNotFoundException {
        Song song = libraryService.getSongById(songId)
                .orElseThrow(() -> new SongNotFoundException(songId));
        return playbackHistorySongRepository.save(new PlaybackHistorySong()
                .setSong(song)
                .setUser(userService.getById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("User '" + userId + "' not found."))));
    }
}
