package net.dorokhov.pony2.core.library.service.filetree.domain;

import net.dorokhov.pony2.api.library.domain.FileType;

import java.io.IOException;

public interface FileNode extends Node {

	FileType getFileType() throws IOException;

	String getChecksum() throws IOException;
}
