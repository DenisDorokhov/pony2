package net.dorokhov.pony2.core.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dorokhov.pony2.api.library.domain.PlaybackHistorySong;
import net.dorokhov.pony2.api.library.domain.PlaybackHistoryStatistics;
import net.dorokhov.pony2.api.library.domain.Song;
import net.dorokhov.pony2.api.library.service.PlaybackHistoryService;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.api.user.service.UserService;
import net.dorokhov.pony2.core.library.repository.PlaybackHistorySongRepository;
import net.dorokhov.pony2.core.library.repository.SongRepository;
import net.dorokhov.pony2.core.library.service.exception.SongNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PlaybackHistoryServiceImpl implements PlaybackHistoryService {

    private static final int MAX_HISTORY_ENTRIES = 300;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final PlaybackHistorySongRepository playbackHistorySongRepository;
    private final SongRepository songRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public PlaybackHistoryServiceImpl(
            PlaybackHistorySongRepository playbackHistorySongRepository,
            SongRepository songRepository,
            UserService userService,
            ObjectMapper objectMapper
    ) {
        this.playbackHistorySongRepository = playbackHistorySongRepository;
        this.songRepository = songRepository;
        this.userService = userService;
        this.objectMapper = objectMapper;
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
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new SongNotFoundException(songId));
        return playbackHistorySongRepository.save(new PlaybackHistorySong()
                .setSong(song)
                .setUser(userService.getById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("User '" + userId + "' not found."))));
    }

    @Transactional
    @Override
    public String backupHistory() {
        Map<String, List<PlaybackHistorySongBackup>> userEmailToBackup = new HashMap<>();
        for (User user : userService.getAll()) {
            List<PlaybackHistorySongBackup> backup = getHistory(user.getId()).stream()
                    .map(PlaybackHistorySongBackup::of)
                    .toList();
            userEmailToBackup.put(user.getEmail(), backup);
        }
        try {
            return objectMapper.writeValueAsString(userEmailToBackup);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    @Override
    public RestoredHistory restoreHistory(String backup) {

        Map<String, List<PlaybackHistorySongBackup>> userEmailToBackup;
        try {
            userEmailToBackup = objectMapper.readValue(backup, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        Set<String> notFoundUserEmails = new TreeSet<>();
        AtomicInteger restoredSongCount = new AtomicInteger(0);
        AtomicInteger notFoundSongCount = new AtomicInteger(0);
        for (String userEmail : userEmailToBackup.keySet()) {
            userService.getByEmail(userEmail).ifPresentOrElse(user -> {
                for (PlaybackHistorySongBackup songBackup : userEmailToBackup.get(userEmail)) {
                    Song foundSong = songRepository.findByPath(songBackup.songPath());
                    if (foundSong != null) {
                        playbackHistorySongRepository.save(new PlaybackHistorySong()
                                .setCreationDate(songBackup.date())
                                .setSong(foundSong)
                                .setUser(user)
                        );
                        restoredSongCount.incrementAndGet();
                    } else {
                        logger.warn("History of user '{}' will not be fully restored: song '{}' not found.", userEmail, songBackup.songPath());
                        notFoundSongCount.incrementAndGet();
                    }
                }
            }, () -> {
                logger.warn("Playlists will not be fully restored: user '{}' not found.", userEmail);
                notFoundUserEmails.add(userEmail);
            });
        }
        return new RestoredHistory(restoredSongCount.get(), notFoundSongCount.get(), new ArrayList<>(notFoundUserEmails));
    }

    record PlaybackHistorySongBackup(
            String songPath,
            LocalDateTime date
    ) {
        static PlaybackHistorySongBackup of(PlaybackHistorySong song) {
            return new PlaybackHistorySongBackup(song.getSong().getPath(), song.getCreationDate());
        }
    }
}
