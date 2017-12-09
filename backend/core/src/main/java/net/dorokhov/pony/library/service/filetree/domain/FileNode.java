package net.dorokhov.pony.library.service.filetree.domain;

import net.dorokhov.pony.library.domain.FileType;

import java.io.IOException;

public interface FileNode extends Node {

	FileType getFileType() throws IOException;

	String getChecksum() throws IOException;
}
