package net.dorokhov.pony.library.service.impl.scan;

import net.dorokhov.pony.library.domain.Song;
import net.dorokhov.pony.library.service.impl.audio.domain.WritableAudioData;
import net.dorokhov.pony.library.service.impl.filetree.domain.AudioNode;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.io.IOException;

@Component
class LibraryImporter {

    public Song importSong(AudioNode audioNode) throws IOException {
        // TODO: implement
        System.out.println("not implemented");
        return null;
    }
    
    @Nullable
    public Song writeAndImportSong(AudioNode audioNode, WritableAudioData audioData) throws IOException {
        // TODO: implement
        System.out.println("not implemented");
        return null;
    }
}
