package net.dorokhov.pony.library.service.internal.filetree.domain;

import net.dorokhov.pony.library.service.internal.image.domain.ImageSize;

import java.io.IOException;

public interface ImageNode extends FileNode {

    ImageSize getImageSize() throws IOException;
}
