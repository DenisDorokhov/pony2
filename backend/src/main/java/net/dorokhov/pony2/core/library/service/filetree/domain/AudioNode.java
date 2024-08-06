package net.dorokhov.pony2.core.library.service.filetree.domain;

import net.dorokhov.pony2.api.library.domain.ReadableAudioData;

import java.io.IOException;

public interface AudioNode extends FileNode {
    ReadableAudioData getAudioData() throws IOException;
}
