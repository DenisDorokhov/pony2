package net.dorokhov.pony.filetree.domain;

import net.dorokhov.pony.audio.domain.ReadableAudioData;

import java.io.IOException;

public interface AudioNode extends FileNode {
    
    ReadableAudioData getAudioData() throws IOException;
}
