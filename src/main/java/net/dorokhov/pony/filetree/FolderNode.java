package net.dorokhov.pony.filetree;

import java.util.Set;

public interface FolderNode extends Node {

    Set<ImageNode> getChildImages(boolean recursive);

    Set<AudioNode> getChildAudios(boolean recursive);

    Set<FolderNode> getChildFolders(boolean recursive);
}
