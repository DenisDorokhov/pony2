package net.dorokhov.pony.filetree;

import net.dorokhov.pony.image.ImageSize;

import java.io.IOException;

public interface ImageNode extends FileNode {

    ImageSize getImageSize() throws IOException;
}
