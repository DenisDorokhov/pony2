package net.dorokhov.pony.library.service.impl.scan;

import net.dorokhov.pony.library.service.impl.filetree.domain.AudioNode;
import net.dorokhov.pony.library.service.impl.filetree.domain.ImageNode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class Cleaner {
    
    public void cleanSongs(List<AudioNode> audioNodes, ProgressObserver progressObserver) {
        // TODO: implement
    }
    
    public void cleanArtworks(List<ImageNode> imageNodes, ProgressObserver progressObserver) {
        // TODO: implement
    }
}
