package net.dorokhov.pony.library.service.impl.scan;

import net.dorokhov.pony.library.domain.Song;
import net.dorokhov.pony.library.service.exception.SongNotFoundException;
import net.dorokhov.pony.library.service.impl.audio.domain.WritableAudioData;
import net.dorokhov.pony.library.service.impl.filetree.domain.AudioNode;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
class Importer {

    public Song importSong(AudioNode audioNode) throws IOException {
        // TODO: implement
        return null;
    }
    
    public Song writeAndImportSong(AudioNode audioNode, WritableAudioData audioData) throws SongNotFoundException, IOException {
        // TODO: implement
        return null;
    }
}
