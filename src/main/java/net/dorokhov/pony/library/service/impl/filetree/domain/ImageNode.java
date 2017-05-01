package net.dorokhov.pony.library.service.impl.filetree.domain;

import net.dorokhov.pony.library.service.impl.image.domain.ImageSize;

import java.io.IOException;

public interface ImageNode extends FileNode {

    ImageSize getImageSize() throws IOException;
}
