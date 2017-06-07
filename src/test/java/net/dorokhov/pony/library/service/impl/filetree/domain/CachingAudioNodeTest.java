package net.dorokhov.pony.library.service.impl.filetree.domain;

import net.dorokhov.pony.library.domain.FileType;
import net.dorokhov.pony.library.service.impl.audio.AudioTagger;
import net.dorokhov.pony.library.service.impl.audio.domain.ReadableAudioData;
import net.dorokhov.pony.library.service.impl.file.ChecksumCalculator;
import net.dorokhov.pony.library.service.impl.file.FileTypeResolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import static net.dorokhov.pony.fixture.ReadableAudioDataFixtures.readableAudioData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CachingAudioNodeTest {
    
    @Mock
    private FileTypeResolver fileTypeResolver;
    
    @Mock
    private ChecksumCalculator checksumCalculator;
    
    @Mock
    private AudioTagger audioTagger;

    @Test
    public void shouldCacheFileType() throws Exception {
        
        FileType fileType = FileType.of("text/plan", "txt");
        when(fileTypeResolver.resolve((File) any())).thenReturn(fileType);
        CachingAudioNode audioNode = new CachingAudioNode(mock(File.class), null, 
                fileTypeResolver, checksumCalculator, audioTagger);
        
        FileType result = audioNode.getFileType();
        assertThat(result).isEqualTo(fileType);
        verify(fileTypeResolver, times(1)).resolve((File) any());
        
        result = audioNode.getFileType();
        assertThat(result).isEqualTo(fileType);
        verify(fileTypeResolver, times(1)).resolve((File) any());
    }

    @Test
    public void shouldCacheChecksum() throws Exception {

        String checksum = "someChecksum";
        when(checksumCalculator.calculate((File) any())).thenReturn(checksum);
        CachingAudioNode audioNode = new CachingAudioNode(mock(File.class), null,
                fileTypeResolver, checksumCalculator, audioTagger);

        String result = audioNode.getChecksum();
        assertThat(result).isEqualTo(checksum);
        verify(checksumCalculator, times(1)).calculate((File) any());

        result = audioNode.getChecksum();
        assertThat(result).isEqualTo(checksum);
        verify(checksumCalculator, times(1)).calculate((File) any());
    }

    @Test
    public void shouldCacheAudioData() throws Exception {

        ReadableAudioData audioData = readableAudioData();
        when(audioTagger.read(any())).thenReturn(audioData);
        CachingAudioNode audioNode = new CachingAudioNode(mock(File.class), null,
                fileTypeResolver, checksumCalculator, audioTagger);

        ReadableAudioData result = audioNode.getAudioData();
        assertThat(result).isEqualTo(audioData);
        verify(audioTagger, times(1)).read(any());

        result = audioNode.getAudioData();
        assertThat(result).isEqualTo(audioData);
        verify(audioTagger, times(1)).read(any());
    }
}