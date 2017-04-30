package net.dorokhov.pony.library.service.internal.artwork.command;

import net.dorokhov.pony.library.service.internal.filetree.domain.ImageNode;

import static com.google.common.base.Preconditions.checkNotNull;

public class ImageNodeArtworkCommand extends ArtworkCommand {

    private final ImageNode imageNode;

    public ImageNodeArtworkCommand(ImageNode imageNode, String sourceUri) {
        super(sourceUri);
        this.imageNode = checkNotNull(imageNode);
    }

    public ImageNode getImageNode() {
        return imageNode;
    }
}
