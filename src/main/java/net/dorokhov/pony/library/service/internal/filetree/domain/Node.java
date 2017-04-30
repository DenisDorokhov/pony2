package net.dorokhov.pony.library.service.internal.filetree.domain;

import java.io.File;
import java.util.Optional;

public interface Node {

	File getFile();

	Optional<FolderNode> getParentFolder();
}
