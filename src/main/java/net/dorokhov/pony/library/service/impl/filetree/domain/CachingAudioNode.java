package net.dorokhov.pony.library.service.impl.filetree.domain;

import net.dorokhov.pony.library.service.impl.audio.AudioTagger;
import net.dorokhov.pony.library.service.impl.audio.domain.ReadableAudioData;
import net.dorokhov.pony.library.service.impl.file.ChecksumCalculator;
import net.dorokhov.pony.library.service.impl.file.FileTypeResolver;

import java.io.File;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public class CachingAudioNode extends AbstractCachingFileNode implements AudioNode {

    private final AudioTagger audioTagger;
    
    private volatile ReadableAudioData audioData = null;
    
    private final Object audioDataLock = new Object();
    
    public CachingAudioNode(File file, FolderNode parentFolder,
                            FileTypeResolver fileTypeResolver, ChecksumCalculator checksumCalculator,
                            AudioTagger audioTagger) {
        super(file, parentFolder, fileTypeResolver, checksumCalculator);
        this.audioTagger = checkNotNull(audioTagger);
    }

    @Override
    public ReadableAudioData getAudioData() throws IOException {
        if (audioData == null) {
            synchronized (audioDataLock) {
                if (audioData == null) {
                    audioData = audioTagger.read(file);
                }
            }
        }
        return audioData;
    }
}
