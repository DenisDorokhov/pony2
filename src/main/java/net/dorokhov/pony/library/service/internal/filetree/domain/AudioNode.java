package net.dorokhov.pony.library.service.internal.filetree.domain;

import net.dorokhov.pony.library.service.internal.audio.domain.ReadableAudioData;

import java.io.IOException;

public interface AudioNode extends FileNode {
    
    ReadableAudioData getAudioData() throws IOException;
}
