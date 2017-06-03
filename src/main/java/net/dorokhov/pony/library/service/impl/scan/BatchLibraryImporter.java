package net.dorokhov.pony.library.service.impl.scan;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.library.domain.Song;
import net.dorokhov.pony.library.service.impl.audio.domain.WritableAudioData;
import net.dorokhov.pony.library.service.impl.filetree.domain.AudioNode;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class BatchLibraryImporter {

    public static class ImportResult {

        private final List<Song> importedSongs;
        private final List<File> failedImports;

        public ImportResult(List<Song> importedSongs, List<File> failedImports) {
            this.importedSongs = ImmutableList.copyOf(importedSongs);
            this.failedImports = ImmutableList.copyOf(failedImports);
        }

        public List<Song> getImportedSongs() {
            return importedSongs;
        }

        public List<File> getFailedImports() {
            return failedImports;
        }
    }

    public static class WriteAndImportCommand {

        private final AudioNode audioNode;
        private final WritableAudioData audioData;

        public WriteAndImportCommand(AudioNode audioNode, WritableAudioData audioData) {
            this.audioNode = checkNotNull(audioNode);
            this.audioData = checkNotNull(audioData);
        }

        public AudioNode getAudioNode() {
            return audioNode;
        }

        public WritableAudioData getAudioData() {
            return audioData;
        }
    }

    public ImportResult readAndImport(List<AudioNode> audioNodes, ProgressObserver observer) {
        // TODO: implement
        return null;
    }

    public ImportResult writeAndImport(List<WriteAndImportCommand> commands, ProgressObserver observer) {
        // TODO: implement
        return null;
    }
}
