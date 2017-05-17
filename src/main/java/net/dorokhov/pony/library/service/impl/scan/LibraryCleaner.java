package net.dorokhov.pony.library.service.impl.scan;

import net.dorokhov.pony.library.service.impl.filetree.domain.AudioNode;
import net.dorokhov.pony.library.service.impl.filetree.domain.ImageNode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class LibraryCleaner {
    
    public void cleanSongs(List<AudioNode> audioNodes, ItemProgressObserver itemProgressObserver) {
        // TODO: implement
        System.out.println("not implemented");
    }
    
    public void cleanArtworks(List<ImageNode> imageNodes, ItemProgressObserver itemProgressObserver) {
        // TODO: implement
        System.out.println("not implemented");
    }
}
