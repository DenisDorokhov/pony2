package net.dorokhov.pony.artwork.draft;

import net.dorokhov.pony.filetree.ImageNode;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class ImageNodeArtworkDraft extends ArtworkDraft {

    private final ImageNode imageNode;

    public ImageNodeArtworkDraft(ImageNode imageNode, String tag) {
        this(imageNode, tag, null);
    }

    public ImageNodeArtworkDraft(ImageNode imageNode, String tag, Map<String, String> metaData) {
        super(tag, metaData);
        checkNotNull(imageNode);
        this.imageNode = imageNode;
    }

    public ImageNode getImageNode() {
        return imageNode;
    }
}
