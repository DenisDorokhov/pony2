package net.dorokhov.pony2.core.library.service.scan;

import net.dorokhov.pony2.api.library.domain.Song;
import net.dorokhov.pony2.core.library.repository.SongRepository;
import net.dorokhov.pony2.core.library.service.filetree.domain.AudioNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

@Component
public class BatchLibraryImportPlanner {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static class Plan {

        private final List<AudioNode> audioNodesToImport;
        private final List<AudioNode> audioNodesToSkip;

        public Plan(List<AudioNode> audioNodesToImport, List<AudioNode> audioNodesToSkip) {
            this.audioNodesToImport = unmodifiableList(audioNodesToImport);
            this.audioNodesToSkip = unmodifiableList(audioNodesToSkip);
        }

        public List<AudioNode> getAudioNodesToImport() {
            return audioNodesToImport;
        }

        public List<AudioNode> getAudioNodesToSkip() {
            return audioNodesToSkip;
        }
    }
    
    private final SongRepository songRepository;

    public BatchLibraryImportPlanner(SongRepository songRepository) {
        this.songRepository = songRepository;
    }

    @Transactional
    public Plan plan(List<AudioNode> audioNodes) {
        List<AudioNode> audioNodesToImport = new ArrayList<>();
        List<AudioNode> audioNodesToSkip = new ArrayList<>();
        for (AudioNode audioNode : audioNodes) {
            Song song = songRepository.findByPath(audioNode.getFile().getAbsolutePath());
            if (song == null || shouldImportSong(song)) {
                audioNodesToImport.add(audioNode);
            } else {
                audioNodesToSkip.add(audioNode);
            }
        }
        return new Plan(audioNodesToImport, audioNodesToSkip);
    }

    private boolean shouldImportSong(Song song) {
        LocalDateTime fileModificationDate = Instant.ofEpochMilli(song.getFile().lastModified())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        LocalDateTime songModificationDate = song.getUpdateDate();
        if (songModificationDate == null) {
            songModificationDate = song.getCreationDate();
        }
        boolean result = songModificationDate.isBefore(fileModificationDate);
        if (result) {
            logger.debug("Song file has been modified, file modification date '{}' is later than song modification date '{}': {}.",
                    fileModificationDate, songModificationDate, song);
        }
        return result;
    }
}
