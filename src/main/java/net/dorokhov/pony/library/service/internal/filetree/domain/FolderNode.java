package net.dorokhov.pony.library.service.internal.filetree.domain;

import java.util.List;

public interface FolderNode extends Node {

    List<ImageNode> getChildImages(boolean recursive);

    List<AudioNode> getChildAudios(boolean recursive);

    List<FolderNode> getChildFolders(boolean recursive);
}
