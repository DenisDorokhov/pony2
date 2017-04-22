package net.dorokhov.pony.artwork.domain;

import net.dorokhov.pony.filetree.domain.ImageNode;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class ImageNodeArtworkDraft extends ArtworkDraft {

    private final ImageNode imageNode;

    public ImageNodeArtworkDraft(ImageNode imageNode, String tag) {
        this(imageNode, tag, null);
    }

    public ImageNodeArtworkDraft(ImageNode imageNode, String tag, Map<String, String> metaData) {
        super(tag, metaData);
        this.imageNode = checkNotNull(imageNode);
    }

    public ImageNode getImageNode() {
        return imageNode;
    }
}
