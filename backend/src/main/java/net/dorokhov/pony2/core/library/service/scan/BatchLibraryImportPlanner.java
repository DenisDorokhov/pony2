package net.dorokhov.pony2.core.library.service.scan;

import net.dorokhov.pony2.core.library.repository.SongRepository;
import net.dorokhov.pony2.core.library.service.filetree.domain.AudioNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        List<String> filePaths = audioNodes.stream()
                .map(node -> node.getFile().getAbsolutePath())
                .toList();
        Map<String, SongRepository.SongFile> pathToSongFile = songRepository.findByPathIn(filePaths).stream()
                .collect(Collectors.toMap(SongRepository.SongFile::path, Function.identity()));
        List<AudioNode> audioNodesToImport = new ArrayList<>();
        List<AudioNode> audioNodesToSkip = new ArrayList<>();
        for (AudioNode audioNode : audioNodes) {
            SongRepository.SongFile song = pathToSongFile.get(audioNode.getFile().getAbsolutePath());
            if (song == null || shouldImportSong(song)) {
                audioNodesToImport.add(audioNode);
            } else {
                audioNodesToSkip.add(audioNode);
            }
        }
        return new Plan(audioNodesToImport, audioNodesToSkip);
    }

    private boolean shouldImportSong(SongRepository.SongFile song) {
        Instant lastModified;
        try {
            lastModified = Instant.ofEpochMilli(Files.getLastModifiedTime(Path.of(song.path())).toMillis());
        } catch (IOException e) {
            throw new RuntimeException("Could not retrieve last modified time for song file '" + song.path() + "'.", e);
        }
        LocalDateTime fileModificationDate = lastModified.atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime songModificationDate = song.updateDate();
        if (songModificationDate == null) {
            songModificationDate = song.creationDate();
        }
        boolean result = songModificationDate.isBefore(fileModificationDate);
        if (result) {
            logger.debug("Song file has been modified, file modification date '{}' is later than song modification date '{}': {}.",
                    fileModificationDate, songModificationDate, song);
        }
        return result;
    }
}
