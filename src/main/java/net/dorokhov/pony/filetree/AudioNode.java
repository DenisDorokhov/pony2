package net.dorokhov.pony.filetree;

import net.dorokhov.pony.audio.ReadableAudioData;

import java.io.IOException;

public interface AudioNode extends FileNode {
    
    ReadableAudioData getAudioData() throws IOException;
}
