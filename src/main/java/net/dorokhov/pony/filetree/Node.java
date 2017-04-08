package net.dorokhov.pony.filetree;

import java.io.File;
import java.util.Optional;

public interface Node {

	File getFile();

	Optional<FolderNode> getParentFolder();
}
