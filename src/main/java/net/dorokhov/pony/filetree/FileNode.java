package net.dorokhov.pony.filetree;

import net.dorokhov.pony.file.FileType;

import java.io.IOException;

public interface FileNode extends Node {

	FileType getType() throws IOException;

	String getChecksum() throws IOException;
}
