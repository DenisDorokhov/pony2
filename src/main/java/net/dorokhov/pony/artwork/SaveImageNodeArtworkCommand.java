package net.dorokhov.pony.artwork;

import net.dorokhov.pony.filetree.ImageNode;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class SaveImageNodeArtworkCommand extends SaveArtworkCommand {

    private final ImageNode imageNode;

    public SaveImageNodeArtworkCommand(ImageNode imageNode, String tag) {
        this(imageNode, tag, null);
    }

    public SaveImageNodeArtworkCommand(ImageNode imageNode, String tag, Map<String, String> metaData) {
        super(tag, metaData);
        checkNotNull(imageNode);
        this.imageNode = imageNode;
    }

    public ImageNode getImageNode() {
        return imageNode;
    }
}
