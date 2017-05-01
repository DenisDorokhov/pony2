package net.dorokhov.pony.library.service.impl.artwork.command;

import net.dorokhov.pony.library.service.impl.filetree.domain.ImageNode;

import static com.google.common.base.Preconditions.checkNotNull;

public class ImageNodeArtworkCommand {
    
    private final String sourceUri;

    private final ImageNode imageNode;

    public ImageNodeArtworkCommand(String sourceUri, ImageNode imageNode) {
        this.sourceUri = checkNotNull(sourceUri);
        this.imageNode = checkNotNull(imageNode);
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public ImageNode getImageNode() {
        return imageNode;
    }
}
