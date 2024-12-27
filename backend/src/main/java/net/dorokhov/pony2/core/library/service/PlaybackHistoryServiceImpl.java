package net.dorokhov.pony2.core.library.service;

import com.google.common.util.concurrent.Striped;
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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Service
public class PlaybackHistoryServiceImpl implements PlaybackHistoryService {

    private static final int MAX_HISTORY_ENTRIES = 300;

    private final PlaybackHistorySongRepository playbackHistorySongRepository;
    private final LibraryService libraryService;
    private final UserService userService;

    private final Striped<Lock> striped;

    public PlaybackHistoryServiceImpl(
            PlaybackHistorySongRepository playbackHistorySongRepository,
            LibraryService libraryService,
            UserService userService
    ) {
        this.playbackHistorySongRepository = playbackHistorySongRepository;
        this.libraryService = libraryService;
        this.userService = userService;
        striped = Striped.lazyWeakLock(10);
    }

    @Override
    public PlaybackHistoryStatistics getStatistics(String userId) {
        return new PlaybackHistoryStatistics()
                .setTotalCount(playbackHistorySongRepository.countByUserId(userId));
    }

    @Override
    public List<PlaybackHistorySong> getHistory(String userId) {
        return playbackHistorySongRepository.findByUserId(userId, PageRequest.of(0, MAX_HISTORY_ENTRIES,
                        Sort.by(Sort.Direction.DESC, "creationDate")))
                .getContent();
    }

    @Override
    public PlaybackHistorySong addSongToHistory(String userId, String songId) throws SongNotFoundException {
        Song song = libraryService.getSongById(songId)
                .orElseThrow(() -> new SongNotFoundException(songId));
        Lock lock = striped.get(userId);
        try {
            if (lock.tryLock(30, TimeUnit.SECONDS)) {
                try {
                    Optional<PlaybackHistorySong> duplicateSong = playbackHistorySongRepository.findByUserId(userId,
                                    PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "creationDate"))
                            ).stream()
                            .filter(lastSong -> lastSong.getSong().getId().equals(songId))
                            .findAny();
                    return duplicateSong.orElseGet(() -> playbackHistorySongRepository.save(new PlaybackHistorySong()
                            .setSong(song)
                            .setUser(userService.getById(userId)
                                    .orElseThrow(() -> new IllegalArgumentException("User '" + userId + "' not found.")))));
                } finally {
                    lock.unlock();
                }
            } else {
                throw new IllegalStateException("Could not acquire lock to add song to history.");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
