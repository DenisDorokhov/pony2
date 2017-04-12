package net.dorokhov.pony.filetree;

import java.util.List;

public interface FolderNode extends Node {

    List<ImageNode> getChildImages(boolean recursive);

    List<AudioNode> getChildAudios(boolean recursive);

    List<FolderNode> getChildFolders(boolean recursive);
}
