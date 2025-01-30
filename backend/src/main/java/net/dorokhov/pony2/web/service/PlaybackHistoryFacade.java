package net.dorokhov.pony2.web.service;

import net.dorokhov.pony2.api.library.domain.Song;
import net.dorokhov.pony2.api.library.service.PlaybackHistoryService;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.core.library.service.exception.SongNotFoundException;
import net.dorokhov.pony2.web.dto.*;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import static java.nio.charset.StandardCharsets.UTF_8;

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

    @Transactional
    public BackupDto backupHistory() {
        return BackupDto.of(playbackHistoryService.backupHistory());
    }

    @Transactional
    public RestoredHistoryDto restoreHistory(MultipartFile backupFile) {
        String backup;
        try {
            backup = readResource(backupFile.getResource());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return RestoredHistoryDto.of(playbackHistoryService.restoreHistory(backup));
    }

    private String readResource(Resource resource) throws IOException {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
    }
}
