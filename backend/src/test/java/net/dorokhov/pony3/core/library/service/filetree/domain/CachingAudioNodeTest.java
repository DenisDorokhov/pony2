package net.dorokhov.pony3.core.library.service.filetree.domain;

import net.dorokhov.pony3.api.library.domain.FileType;
import net.dorokhov.pony3.api.library.domain.ReadableAudioData;
import net.dorokhov.pony3.core.library.service.AudioTagger;
import net.dorokhov.pony3.core.library.service.file.ChecksumCalculator;
import net.dorokhov.pony3.core.library.service.file.FileTypeResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;

import static net.dorokhov.pony3.test.ReadableAudioDataFixtures.readableAudioData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CachingAudioNodeTest {
    
    @Mock
    private FileTypeResolver fileTypeResolver;
    
    @Mock
    private ChecksumCalculator checksumCalculator;
    
    @Mock
    private AudioTagger audioTagger;

    @Test
    public void shouldCacheFileType() throws IOException {
        
        FileType fileType = FileType.of("text/plan", "txt");
        when(fileTypeResolver.resolve((File) any())).thenReturn(fileType);
        CachingAudioNode audioNode = new CachingAudioNode(mock(File.class), null, 
                fileTypeResolver, checksumCalculator, audioTagger);
        
        FileType result = audioNode.getFileType();

        assertThat(result).isSameAs(fileType);
        verify(fileTypeResolver, times(1)).resolve((File) any());

        assertThat(audioNode.getFileType()).isSameAs(result);
        verify(fileTypeResolver, times(1)).resolve((File) any());
    }

    @Test
    public void shouldCacheChecksum() throws IOException {

        String checksum = "someChecksum";
        when(checksumCalculator.calculate((File) any())).thenReturn(checksum);
        CachingAudioNode audioNode = new CachingAudioNode(mock(File.class), null,
                fileTypeResolver, checksumCalculator, audioTagger);

        String result = audioNode.getChecksum();

        assertThat(result).isSameAs(checksum);
        verify(checksumCalculator, times(1)).calculate((File) any());

        assertThat(audioNode.getChecksum()).isSameAs(result);
        verify(checksumCalculator, times(1)).calculate((File) any());
    }

    @Test
    public void shouldCacheAudioData() throws IOException {

        ReadableAudioData audioData = readableAudioData();
        when(audioTagger.read(any())).thenReturn(audioData);
        CachingAudioNode audioNode = new CachingAudioNode(mock(File.class), null,
                fileTypeResolver, checksumCalculator, audioTagger);

        ReadableAudioData result = audioNode.getAudioData();

        assertThat(result).isSameAs(audioData);
        verify(audioTagger, times(1)).read(any());

        assertThat(audioNode.getAudioData()).isSameAs(result);
        verify(audioTagger, times(1)).read(any());
    }
}