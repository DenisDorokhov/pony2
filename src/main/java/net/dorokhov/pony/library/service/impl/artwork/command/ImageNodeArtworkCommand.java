package net.dorokhov.pony.library.service.impl.artwork.command;

import net.dorokhov.pony.library.service.impl.filetree.domain.ImageNode;

import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ImageNodeArtworkCommand {
    
    private final URI sourceUri;

    private final ImageNode imageNode;

    public ImageNodeArtworkCommand(URI sourceUri, ImageNode imageNode) {
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
