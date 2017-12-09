package net.dorokhov.pony.library.service.filetree.domain;

import net.dorokhov.pony.library.service.image.domain.ImageSize;

import java.io.IOException;

public interface ImageNode extends FileNode {

    ImageSize getImageSize() throws IOException;
}
