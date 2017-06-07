package net.dorokhov.pony.library.service.impl.filetree.domain;

import net.dorokhov.pony.library.domain.FileType;
import net.dorokhov.pony.library.service.impl.file.ChecksumCalculator;
import net.dorokhov.pony.library.service.impl.file.FileTypeResolver;
import net.dorokhov.pony.library.service.impl.image.ImageSizeReader;
import net.dorokhov.pony.library.service.impl.image.domain.ImageSize;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CachingImageNodeTest {

    @Mock
    private FileTypeResolver fileTypeResolver;

    @Mock
    private ChecksumCalculator checksumCalculator;
    
    @Mock
    private ImageSizeReader imageSizeReader;

    @Test
    public void shouldCacheFileType() throws Exception {

        FileType fileType = FileType.of("text/plan", "txt");
        when(fileTypeResolver.resolve((File) any())).thenReturn(fileType);
        CachingImageNode imageNode = new CachingImageNode(mock(File.class), null,
                fileTypeResolver, checksumCalculator, imageSizeReader);

        FileType result = imageNode.getFileType();
        assertThat(result).isEqualTo(fileType);
        verify(fileTypeResolver, times(1)).resolve((File) any());

        result = imageNode.getFileType();
        assertThat(result).isEqualTo(fileType);
        verify(fileTypeResolver, times(1)).resolve((File) any());
    }

    @Test
    public void shouldCacheChecksum() throws Exception {

        String checksum = "someChecksum";
        when(checksumCalculator.calculate((File) any())).thenReturn(checksum);
        CachingImageNode imageNode = new CachingImageNode(mock(File.class), null,
                fileTypeResolver, checksumCalculator, imageSizeReader);

        String result = imageNode.getChecksum();
        assertThat(result).isEqualTo(checksum);
        verify(checksumCalculator, times(1)).calculate((File) any());

        result = imageNode.getChecksum();
        assertThat(result).isEqualTo(checksum);
        verify(checksumCalculator, times(1)).calculate((File) any());
    }

    @Test
    public void shouldCacheImageSize() throws Exception {

        ImageSize imageSize = ImageSize.of(100, 100);
        when(imageSizeReader.read((File) any())).thenReturn(imageSize);
        CachingImageNode imageNode = new CachingImageNode(mock(File.class), null,
                fileTypeResolver, checksumCalculator, imageSizeReader);

        ImageSize result = imageNode.getImageSize();
        assertThat(result).isEqualTo(imageSize);
        verify(imageSizeReader, times(1)).read((File) any());

        result = imageNode.getImageSize();
        assertThat(result).isEqualTo(imageSize);
        verify(imageSizeReader, times(1)).read((File) any());
    }
}