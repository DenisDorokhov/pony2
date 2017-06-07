package net.dorokhov.pony.library.service.impl.filetree.domain;

import net.dorokhov.pony.library.service.impl.audio.domain.ReadableAudioData;

import java.io.IOException;

public interface AudioNode extends FileNode {
    ReadableAudioData getAudioData() throws IOException;
}
