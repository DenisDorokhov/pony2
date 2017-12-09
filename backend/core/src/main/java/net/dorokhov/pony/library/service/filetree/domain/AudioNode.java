package net.dorokhov.pony.library.service.filetree.domain;

import net.dorokhov.pony.library.domain.ReadableAudioData;

import java.io.IOException;

public interface AudioNode extends FileNode {
    ReadableAudioData getAudioData() throws IOException;
}
