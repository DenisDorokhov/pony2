package net.dorokhov.pony2.core.library.service.artwork.command;

import net.dorokhov.pony2.core.library.service.filetree.domain.ImageNode;

import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ImageNodeArtworkStorageCommand {
    
    private final URI sourceUri;

    private final ImageNode imageNode;

    public ImageNodeArtworkStorageCommand(URI sourceUri, ImageNode imageNode) {
        this.sourceUri = checkNotNull(sourceUri);
        this.imageNode = checkNotNull(imageNode);
    }

    public URI getSourceUri() {
        return sourceUri;
    }

    public ImageNode getImageNode() {
        return imageNode;
    }
}
