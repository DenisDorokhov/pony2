package net.dorokhov.pony.core.library.service.filetree.domain;

import net.dorokhov.pony.api.library.domain.ReadableAudioData;

import java.io.IOException;

public interface AudioNode extends FileNode {
    ReadableAudioData getAudioData() throws IOException;
}
