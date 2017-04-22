package net.dorokhov.pony.filetree.domain;

import net.dorokhov.pony.file.domain.FileType;

import java.io.IOException;

public interface FileNode extends Node {

	FileType getFileType() throws IOException;

	String getChecksum() throws IOException;
}
