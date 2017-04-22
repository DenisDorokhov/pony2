package net.dorokhov.pony.filetree.domain;

import net.dorokhov.pony.image.domain.ImageSize;

import java.io.IOException;

public interface ImageNode extends FileNode {

    ImageSize getImageSize() throws IOException;
}
