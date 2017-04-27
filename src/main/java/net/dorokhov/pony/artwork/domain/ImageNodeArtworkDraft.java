package net.dorokhov.pony.artwork.domain;

import net.dorokhov.pony.filetree.domain.ImageNode;

import static com.google.common.base.Preconditions.checkNotNull;

public class ImageNodeArtworkDraft extends ArtworkDraft {

    private final ImageNode imageNode;

    public ImageNodeArtworkDraft(ImageNode imageNode, String sourceUri) {
        super(sourceUri);
        this.imageNode = checkNotNull(imageNode);
    }

    public ImageNode getImageNode() {
        return imageNode;
    }
}
